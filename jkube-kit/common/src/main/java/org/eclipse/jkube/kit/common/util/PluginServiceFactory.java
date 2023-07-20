/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A simple factory for creating services with predefined constructors (with <code>&lt;C&gt;</code> argument)
 * from a textual descriptor. This descriptor, which must be a resource loadable by this class'
 * classloader, is a plain text file which looks like
 *
 * <pre>{@code
 *   com.example.MyProjectLabelEnricher
 *   !org.eclipse.jkube.maven.jkube.enhancer.DefaultProjectLabelEnricher
 *   com.example.AnotherEnricher,50
 * }</pre>
 *
 * If a line starts with <code>!</code> it is removed if it has been added previously.
 * The optional second numeric value is the order in which the services are returned.
 *
 */
public final class PluginServiceFactory<C> {

    private static final int DEFAULT_ORDER = 100;
    // Matches comment lines and empty lines. these are skipped
    private static final Pattern COMMENT_LINE_PATTERN = Pattern.compile("^(\\s*#.*|\\s*)$");
    private final List<ClassLoader> additionalClassLoaders;

    // Parameters for service constructors
    private final C constructorParameter;

    public PluginServiceFactory(C constructorParameter, ClassLoader ... loaders) {
        this.constructorParameter = constructorParameter;
        this.additionalClassLoaders = new ArrayList<>();
        Stream.of(loaders).forEach(this::addAdditionalClassLoader);
    }

    /**
     * Create a list of services ordered according to the ordering given in the
     * service descriptor files. Note, that the descriptor will be looked up
     * in the whole classpath space, which can result in reading in multiple
     * descriptors with a single path. Note, that the reading order for multiple
     * resources with the same name is not defined.
     *
     * @param descriptorPaths a list of resource paths which are handle in the given order.
     *        Normally, default service should be given as first parameter so that custom
     *        descriptors have a chance to remove a default service.
     * @param <T> type of the service objects to create
     * @return an ordered list of created services or an empty list.
     */
    public <T> List<T> createServiceObjects(String... descriptorPaths) {
        try {
            ServiceEntry.initDefaultOrder();
            TreeMap<ServiceEntry,T> serviceMap = new TreeMap<>();
            for (String descriptor : descriptorPaths) {
                readServiceDefinitions(serviceMap, descriptor);
            }
            return new ArrayList<>(serviceMap.values());
        } finally {
            ServiceEntry.removeDefaultOrder();
        }
    }

    private <T> void readServiceDefinitions(Map<ServiceEntry, T> extractorMap, String defPath) {
        try {
            for (String url : ClassUtil.getResources(defPath, additionalClassLoaders)) {
                readServiceDefinitionFromUrl(extractorMap, url);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load service from " + defPath + ": " + e, e);
        }
    }

    private <T> void readServiceDefinitionFromUrl(Map<ServiceEntry, T> extractorMap, String url) {
        String line = null;
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8))) {
            line = reader.readLine();
            while (line != null) {
                createOrRemoveService(extractorMap, line);
                line = reader.readLine();
            }
        } catch (ReflectiveOperationException|IOException e) {
            throw new IllegalStateException("Cannot load service " + line + " defined in " +
                    url + " : " + e + ". Aborting", e);
        }
    }


    private synchronized  <T> void createOrRemoveService(Map<ServiceEntry, T> serviceMap, String line)
            throws ReflectiveOperationException {
        if (line.length() > 0 && !COMMENT_LINE_PATTERN.matcher(line).matches()) {
            ServiceEntry entry = new ServiceEntry(line);
            if (entry.isRemove()) {
                // Removing is a bit complex since we need to find out
                // the proper key since the order is part of equals/hash,
                // so we can't fetch/remove it directly
                Set<ServiceEntry> toRemove = new HashSet<>();
                for (ServiceEntry key : serviceMap.keySet()) {
                    if (key.getClassName().equals(entry.getClassName())) {
                        toRemove.add(key);
                    }
                }
                for (ServiceEntry key : toRemove) {
                    serviceMap.remove(key);
                }
            } else {
                Class<T> clazz = ClassUtil.classForName(entry.getClassName(), additionalClassLoaders);
                if (clazz == null) {
                    throw new ClassNotFoundException("Class " + entry.getClassName() + " could not be found");
                }
                final Constructor<T> constructor = findConstructor(clazz);
                final T service = constructorParameter == null ?
                  constructor.newInstance() : constructor.newInstance(constructorParameter);
                serviceMap.put(entry, service);
            }
        }
    }

    public void addAdditionalClassLoader(ClassLoader classLoader) {
        this.additionalClassLoaders.add(classLoader);
    }

    private <T> Constructor<T> findConstructor(Class<T> clazz) {
        if (constructorParameter == null) {
            try {
                return clazz.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Cannot load service " + clazz.getName());
            }
        }
        final List<Class<?>> types = new ArrayList<>();
        types.add(constructorParameter.getClass());
        types.addAll(getInterfacesForClass(constructorParameter.getClass()));
        for (Class<?> type : types) {
            try {
                return clazz.getConstructor(type);
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        throw new IllegalStateException("Cannot load service " + clazz.getName());
    }

    private List<Class<?>> getInterfacesForClass(Class<?> clazz) {
        List<Class<?>> interfacesList = new ArrayList<>();
        for (Class<?> type : clazz.getInterfaces()) {
            interfacesList.add(type);
            interfacesList.addAll(getInterfacesForClass(type));
        }
        return interfacesList;
    }

    static class ServiceEntry implements Comparable<ServiceEntry> {

        private final String className;
        private final boolean remove;
        private Integer order;

        /**
         * Initialise with start value for entries without an explicit order.
         */
        private static final ThreadLocal<Integer> DEFAULT_ORDER_HOLDER = ThreadLocal.withInitial(() -> DEFAULT_ORDER);

        /**
         * Parse an entry in the service definition. This should be the full qualified classname
         * of a service, optional prefixed with "<code>!</code>" in which case the service is removed
         * from the default list. An order value can be appended after the classname with a comma for give an
         * indication for the ordering of services. If not given, 100 is taken for the first entry, counting up.
         *
         * @param line line to parse
         */
        public ServiceEntry(String line) {
            String[] parts = line.split(",");
            if (parts[0].startsWith("!")) {
                remove = true;
                className = parts[0].substring(1);
            } else {
                remove = false;
                className = parts[0];
            }
            if (parts.length > 1) {
                try {
                    order = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exp) {
                    order = nextDefaultOrder();
                }
            } else {
                order = nextDefaultOrder();
            }
        }

        private Integer nextDefaultOrder() {
            Integer defaultOrder = DEFAULT_ORDER_HOLDER.get();
            DEFAULT_ORDER_HOLDER.set(defaultOrder + 1);
            return defaultOrder;
        }

        private static void initDefaultOrder() {
            DEFAULT_ORDER_HOLDER.set(DEFAULT_ORDER);
        }

        private static void removeDefaultOrder() {
            DEFAULT_ORDER_HOLDER.remove();
        }

        private String getClassName() {
            return className;
        }

        private boolean isRemove() {
            return remove;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            ServiceEntry that = (ServiceEntry) o;

            return className.equals(that.className);

        }

        @Override
        public int hashCode() {
            return className.hashCode();
        }

        /** {@inheritDoc} */
        public int compareTo(ServiceEntry o) {
            int ret = this.order - o.order;
            return ret != 0 ? ret : this.className.compareTo(o.className);
        }
    }
}

