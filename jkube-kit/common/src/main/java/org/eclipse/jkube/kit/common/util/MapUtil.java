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


import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;

public class MapUtil {

    private static final BiFunction<String, Object, Object> GET_OR_NEW = (nK, nV) ->
        nV == null ?  new LinkedHashMap<>() : nV;

    private MapUtil() {}

    /**
     * Add all values of a map to another map, but only if not already existing.
     * @param map target map
     * @param toMerge the values to add
     */
    public static void mergeIfAbsent(Map<String, String> map, Map<String, String> toMerge) {
        for (Map.Entry<String, String> entry : toMerge.entrySet()) {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns a new map with all the entries of first map with rest map entries which don't override map1.
     *
     * Can handle either maps being null. Always returns a new mutable map.
     *
     * <b>Note:</b> Be careful about the ordering of maps passed here. First map passed in the var args
     * would always be given precedence over other maps in case there are colliding entries with same key values.
     *
     * @param maps var arg for maps
     * @param <K> first type
     * @param <V> second type
     * @return merged hash map
     */
    @SafeVarargs
    public static <K,V> Map<K,V> mergeMaps(Map<K, V>... maps) {
        Map<K, V> answer = new HashMap<>();
        for (int i = maps.length-1; i >= 0; i--) {
            if (maps[i] != null) {
                answer.putAll(maps[i]);
            }
        }
        return answer;

    }

    /**
     * Returns a new map with all the entries the provided properties merged.
     *
     * The first arguments take precedence over the later ones.
     * i.e. properties defined in the last argument will not override properties defined in the first argument.
     *
     * @param properties var arg for properties
     * @return merged hash map
     */
    public static Map<String, String> mergeMaps(Properties... properties) {
        Map<String, String> answer = new HashMap<>();
        for (int i = properties.length-1; i >= 0; i--) {
            if (properties[i] != null) {
                answer.putAll(PropertiesUtil.toMap(properties[i]));
            }
        }
        return answer;
    }

    /**
     * Copies all the elements i.e., the mappings, from toPut map into ret, if toPut isn't null.
     * @param ret target hash map
     * @param toPut source hash map
     */
    public static void putAllIfNotNull(Map<String, String> ret, Map<String, String> toPut) {
        if (toPut != null) {
            ret.putAll(toPut);
        }
    }

    /**
     * Build a flattened representation of provided Map.
     *
     * <p> <i>The conversion is compliant with the thorntail spring-boot rules.</i>
     *
     * <p> Given a Map of Maps:
     * <pre>{@code
     * Collections.singletonMap("key", Collections.singletonMap("nested-key", "value"));
     * }</pre>
     *
     * <p> It will return a Map with the following structure
     * <pre>{@code
     * Collections.singletonMap("key.nested-key", "value");
     * }</pre>
     * @param source map of maps
     * @return map with merged nested-keys
     */
    public static Map<String, Object> getFlattenedMap(Map<?, ?> source) {
        return buildFlattenedMap(source, null);
    }

    /**
     * Build a nested representation of the provided Map.
     *
     * <p> Given a Map with a flat structure, it returns a Map of nested Maps. The original keys are split by the dot
     * (<code>.</code>) character. For each element, a new Map node is created.
     *
     * <p> Given the following YAML representation of a Map:
     * <pre>{@code
     * one.two.key: value
     * one.two.three: other
     * }</pre>
     *
     * <p> It will converted to:
     * <pre>{@code
     * one:
     *   two:
     *     key: value
     *     three: other
     * }</pre>
     * @param flattenedMap map with a flat structure
     * @return converted nested map
     */
    public static Map<String, Object> getNestedMap(Map<String, ?> flattenedMap) {
        final Map<String, Object> result = new LinkedHashMap<>();
        flattenedMap.forEach((k, v) -> {
            final String[] nodes = k.split("\\.");
            if (nodes.length == 1) {
                result.put(k, v);
            } else {
                Map<String, Object> currentNode = result;
                for (int it = 0; it < nodes.length - 1; it++){
                    final Object tempNode = currentNode.compute(nodes[it], GET_OR_NEW);
                    if (!(tempNode instanceof Map)) {
                        throw new IllegalArgumentException("The provided input Map is invalid (node <" +
                            nodes[it] + "> overlaps with key)");
                    }
                    currentNode = (Map<String, Object>) tempNode;
                }
                final Object previousNodeValue = currentNode.put(nodes[nodes.length -1], v);
                if (previousNodeValue != null) {
                    throw new IllegalArgumentException("The provided input Map is invalid (key <" +
                        nodes[nodes.length -1] + "> overlaps with node)");
                }
            }
        });
        return result;
    }

    private static Map<String, Object> buildFlattenedMap(Map<?, ?> source, String keyPrefix) {
        final Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = applicableKey(String.valueOf(entry.getKey()), keyPrefix);
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.putAll(buildFlattenedMap((Map<?, ?>) value, key));
            }
            else if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                int count = 0;
                for (Object object : collection) {
                    result.putAll(buildFlattenedMap(Collections.singletonMap("[" + (count++) + "]", object), key));
                }
            }
            else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
        return result;
    }

    private static String applicableKey(String key, String keyPrefix) {
        if (StringUtils.isNotBlank(keyPrefix)) {
            if (key.startsWith("[")) {
                key = keyPrefix + key;
            }
            else {
                key = keyPrefix + "." + key;
            }
        }
        return key;
    }
}

