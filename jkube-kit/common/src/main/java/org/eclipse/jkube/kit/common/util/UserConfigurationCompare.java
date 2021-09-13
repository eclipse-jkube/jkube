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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods to compare the user configuration on entities
 */
public class UserConfigurationCompare {
    private static final Logger LOG = LoggerFactory.getLogger(UserConfigurationCompare.class);

    protected static final Set<String> ignoredProperties = new HashSet<>(Collections.singletonList("status"));

    private UserConfigurationCompare() { }

    /**
     * This method detects if the user has changed the configuration of an entity.
     *
     * It compares the <b>user</b> configuration of 2 object trees ignoring any
     * runtime status or timestamp information.
     *
     * @param entity1 first Object
     * @param entity2 second Object
     * @return true if the configurations are equal.
     */
    public static boolean configEqual(Object entity1, Object entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        } else if ((entity1 instanceof Map) || (entity2 instanceof Map)) {
            return configEqualMap(castTo(Map.class, entity1), castTo(Map.class, entity2));
        } else if ((entity1 instanceof ObjectMeta) || (entity2 instanceof ObjectMeta)) {
            return configEqualObjectMeta(castTo(ObjectMeta.class, entity1), castTo(ObjectMeta.class, entity2));
        } else if (entity1 instanceof Collection && entity2 instanceof Collection) {
            return collectionsEqual((Collection) entity1, (Collection) entity2);
        } else {
            Class<?> aClass = getCommonDenominator(entity1.getClass(), entity2.getClass());
            if (aClass != null) {
                Object castEntity2 = castTo(aClass, entity2);
                if (castEntity2 == null) {
                    return false;
                } else if (aClass.getPackage().getName().startsWith("org.eclipse.jkube")) {
                    return configEqualKubernetesDTO(entity1, entity2, aClass);
                }
            }
            return java.util.Objects.equals(entity1, entity2);
        }
    }

    static <L,R> boolean collectionsEqual(Collection<L> left, Collection<R> right) {
        return subCollection(left, right) && subCollection(right, left);
    }


    static <L, R> boolean itemExists(L item, Collection<R> collection) {
        for (R candidate : collection) {
            if (configEqual(item, candidate)) {
                return true;
            }
        }
        return false;
    }

    static <L, R> boolean subCollection(Collection<L> left, Collection<R> right) {
        for (L candidate : left) {
            if (!itemExists(candidate, right)) {
                return false;
            }
        }
        return true;
    }

    static Class getCommonDenominator(Class left, Class right) {
        if (left.equals(right)) {
            return left;
        } else if (left.isAssignableFrom(right)) {
            return getCommonDenominator(left, right.getSuperclass());
        } else if (right.isAssignableFrom(left)) {
            return getCommonDenominator(left.getSuperclass(), right);
        } else {
            //no match
            return null;
        }
    }

    /**
     * Compares 2 instances of the given Kubernetes DTO class to see if the user has changed their configuration.
     *
     * This method will ignore properties {@link #ignoredProperties} such as status or timestamp properties
     *
     * @param entity1 first entity
     * @param entity2 second entity
     * @param clazz class name
     * @return returns boolean value indicating equality or not.
     */
    protected static boolean configEqualKubernetesDTO(@NotNull Object entity1, @NotNull Object entity2, @NotNull Class<?> clazz) {
        // let's iterate through the objects making sure we've not
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            LOG.warn("Failed to get beanInfo for " + clazz.getName() + ". " + e, e);
            return false;
        }
        try {
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                String name = propertyDescriptor.getName();
                if (ignoredProperties.contains(name)) {
                    continue;
                }
                Method readMethod = propertyDescriptor.getReadMethod();
                if (readMethod != null) {
                    Object value1 = invokeMethod(entity1, readMethod);
                    Object value2 = invokeMethod(entity2, readMethod);
                    if (value1 != null && value2 != null && !configEqual(value1, value2)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected static Object invokeMethod(@NotNull Object entity, Method readMethod) throws InvocationTargetException, IllegalAccessException {
        try {
            return readMethod.invoke(entity);
        } catch (Exception e) {
            LOG.warn("Failed to invoke method " + readMethod + " on " + entity + ". " + e, e);
            throw e;
        }
    }

    protected static boolean configEqualObjectMeta(ObjectMeta entity1, ObjectMeta entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        // TODO should we ignore annotations?
        return java.util.Objects.equals(entity1.getName(), entity2.getName()) &&
                java.util.Objects.equals(entity1.getNamespace(), entity2.getNamespace()) &&
                configEqualMap(entity1.getLabels(), entity2.getLabels()) &&
                configEqualMap(entity1.getAnnotations(), entity2.getAnnotations());
    }

    private static <T> T castTo(Class<T> clazz, Object entity) {
        if (clazz.isInstance(entity)) {
            return clazz.cast(entity);
        } else {
            if (entity != null) {
                LOG.warn("Invalid class {} when expecting {} for instance: {}",
                        entity.getClass().getName(),
                        clazz.getName(),
                        entity);
            }
            return null;
        }
    }

    protected static boolean configEqualMap(Map entity1, Map entity2) {
        if (entity1 == entity2) {
            return true;
        } else if (entity1 == null || entity2 == null) {
            return false;
        }
        int size1 = size(entity1);
        int size2 = size(entity2);
        if (size1 != size2) {
            return false;
        }
        Set<Map.Entry> entries = entity1.entrySet();
        for (Map.Entry entry : entries) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            Object value2 = entity2.get(key);
            if (!configEqual(value, value2)) {
                return false;
            }
        }
        return true;
    }

    protected static int size(Map map) {
        return (map == null) ? 0 : map.size();
    }

    protected static int size(Collection coll) {
        return (coll == null) ? 0 : coll.size();
    }

}
