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


import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtil {

    private MapUtil() {}

    /**
     * Add all values of a map to another map, but only if not already existing.
     * @param map target map
     * @param toMerge the values to ad
     */
    public static void mergeIfAbsent(Map<String, String> map, Map<String, String> toMerge) {
        for (Map.Entry<String, String> entry : toMerge.entrySet()) {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns a new map with all the entries of map1 and any from map2 which don't override map1.
     *
     * Can handle either maps being null. Always returns a new mutable map
     *
     * @param map1 first hash map
     * @param map2 second hash map
     * @param <K> first type
     * @param <V> second type
     * @return merged hash map
     */
    public static <K,V> Map<K,V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> answer = new HashMap<>();
        if (map2 != null) {
            answer.putAll(map2);
        }
        if (map1 != null) {
            answer.putAll(map1);
        }
        return answer;

    }

    /**
     * Copies all of the elements i.e., the mappings, from toPut map into ret, if toPut isn't null.
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
     */
    public static Map<String, Object> getFlattenedMap(Map<?, ?> source) {
        return buildFlattenedMap(source, null);
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

