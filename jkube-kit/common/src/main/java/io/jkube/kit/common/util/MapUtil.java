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
package io.jkube.kit.common.util;


import java.util.HashMap;
import java.util.Map;

public class MapUtil {

    private MapUtil() {}

    /**
     * Adds the given key and value pair into the map if the map does not already contain a value for that key
     *
     * @param map hashmap provided
     * @param name name entry
     * @param value value entry
     */
    public static void putIfAbsent(Map<String, String> map, String name, String value) {
        if (!map.containsKey(name)) {
            map.put(name, value);
        }
    }

    /**
     * Add all values of a map to another map, but onlfy if not already existing.
     * @param map target map
     * @param toMerge the values to ad
     */
    public static void mergeIfAbsent(Map<String, String> map, Map<String, String> toMerge) {
        for (Map.Entry<String, String> entry : toMerge.entrySet()) {
            putIfAbsent(map, entry.getKey(), entry.getValue());;
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

}

