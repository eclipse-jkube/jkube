/**
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jkube.kit.common.KitLogger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author roland
 * @since 24/07/16
 */
public class ClassUtil {

    private ClassUtil() { }

    public static Set<String> getResources(String resource) throws IOException {
        return getResources(resource, null);
    }

    public static Set<String> getResources(String resource, List<ClassLoader> additionalClassLoaders) throws IOException {

        ClassLoader[] classLoaders = mergeClassLoaders(additionalClassLoaders);

        Set<String> ret = new HashSet<>();
        for (ClassLoader cl : classLoaders) {
            Enumeration<URL> urlEnum = cl.getResources(resource);
            ret.addAll(extractUrlAsStringsFromEnumeration(urlEnum));
        }
        return ret;
    }

    private static ClassLoader[] mergeClassLoaders(List<ClassLoader> additionalClassLoaders) {
        ClassLoader[] classLoaders;

        if (additionalClassLoaders != null && !additionalClassLoaders.isEmpty()) {
            classLoaders = ArrayUtils.addAll(getClassLoaders(), additionalClassLoaders.toArray(new ClassLoader[additionalClassLoaders.size()]));
        }
        else {
            classLoaders = getClassLoaders();
        }
        return classLoaders;
    }


    private static ClassLoader[] getClassLoaders() {
        return new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                PluginServiceFactory.class.getClassLoader()
        };
    }

    private static Set<String> extractUrlAsStringsFromEnumeration(Enumeration<URL> urlEnum) {
        Set<String> ret = new HashSet<>();
        while (urlEnum.hasMoreElements()) {
            ret.add(urlEnum.nextElement().toExternalForm());
        }
        return ret;
    }


    public static <T> Class<T> classForName(String className, List<ClassLoader> additionalClassLoaders) {
        ClassLoader[] classLoaders = mergeClassLoaders(additionalClassLoaders);
        Set<ClassLoader> tried = new HashSet<>();
        for (ClassLoader loader : classLoaders) {
            // Go up the classloader stack to eventually find the server class. Sometimes the WebAppClassLoader
            // hide the server classes loaded by the parent class loader.
            while (loader != null) {
                try {
                    if (!tried.contains(loader)) {
                        return (Class<T>) Class.forName(className, true, loader);
                    }
                } catch (ClassNotFoundException ignored) {}
                tried.add(loader);
                loader = loader.getParent();
            }
        }
        return null;
    }


    /**
     * Find all classes below a certain directory which contain
     * main() classes
     *
     * @param rootDir the directory to start from
     * @return List of classes with "public void static main(String[] args)" methods. Can be empty, but not null.
     * @exception IOException if something goes wrong
     */
    public static List<String> findMainClasses(File rootDir) throws IOException {
        List<String> ret = new ArrayList<>();
        if (!rootDir.exists()) {
            return ret;
        }
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("Path %s is not a directory",rootDir.getPath()));
        }
        findClasses(ret, rootDir, rootDir.getAbsolutePath() + "/");
        return ret;
    }

    public static URLClassLoader createClassLoader(List<String> classpathElements, String... paths) {
        List<URL> urls = new ArrayList<>();
        for (String path : paths) {
            URL url = pathToUrl(path);
            urls.add(url);
        }
        for (Object object : classpathElements) {
            if (object != null) {
                String path = object.toString();
                URL url = pathToUrl(path);
                urls.add(url);
            }
        }
        return createURLClassLoader(urls);
    }

    // ========================================================================

    private static URL pathToUrl(String path) {
        try {
            File file = new File(path);
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Cannot convert %s to a an URL: %s",path,e.getMessage()),e);
        }
    }

    private static URLClassLoader createURLClassLoader(Collection<URL> jars) {
        return new URLClassLoader(jars.toArray(new URL[jars.size()]));
    }

    private static final FileFilter DIR_FILTER = pathname -> pathname.isDirectory() && !pathname.getName().startsWith(".");

    private static final FileFilter CLASS_FILE_FILTER = file -> (file.isFile() && file.getName().endsWith(".class"));


    private static void findClasses(List<String> classes, File dir, String prefix) throws IOException {
        for (File subDir : dir.listFiles(DIR_FILTER)) {
            findClasses(classes, subDir, prefix);
        }

        for (File classFile : dir.listFiles(CLASS_FILE_FILTER)) {
            try (InputStream is = new FileInputStream(classFile)) {
                if (hasMainMethod(is)) {
                    classes.add(convertToClass(classFile.getAbsolutePath(), prefix));
                }
            }
        }
    }

    private static boolean hasMainMethod(InputStream is) throws IOException {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.makeClass(is);
            CtClass stringClass = pool.get("java.lang.String[]");
            CtMethod mainMethod = ctClass.getDeclaredMethod("main", new CtClass[] { stringClass });
            return mainMethod.getReturnType() == CtClass.voidType &&
                    Modifier.isStatic(mainMethod.getModifiers()) &&
                    Modifier.isPublic(mainMethod.getModifiers());
        } catch (NotFoundException e) {
            return false;
        }
    }

    private static String convertToClass(String name, String prefix) {
        String ret = name.replaceAll("[/\\\\]", ".");
        ret = ret.substring(0, name.length() - ".class".length());
        return ret.substring(prefix.length());
    }


    public static URLClassLoader createProjectClassLoader(List<String> elements, KitLogger log) {

        try {

            List<URL> compileJars = new ArrayList<>();

            for (String element : elements) {
                compileJars.add(new File(element).toURI().toURL());
            }

            return new URLClassLoader(compileJars.toArray(new URL[compileJars.size()]),
                    PluginServiceFactory.class.getClassLoader());

        } catch (Exception e) {
            log.warn("Instructed to use project classpath, but cannot. Continuing build if we can: ", e);
        }

        // return an empty CL .. don't want to have to deal with NULL later
        // if somehow we incorrectly call this method
        return new URLClassLoader(new URL[]{});
    }
}

