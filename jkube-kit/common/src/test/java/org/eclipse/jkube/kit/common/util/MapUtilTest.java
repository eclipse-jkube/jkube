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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.kit.common.util.MapUtil.getFlattenedMap;
import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 05/08/16
 */
public class MapUtilTest {

    @Test
    public void testMergeIfAbsent() {
        Map<String, String> origMap = createMap("eins", "one", "zwei", "two");
        Map<String, String> toMergeMap = createMap("zwei", "deux", "drei", "trois");
        Map<String, String> expected = createMap("eins", "one", "zwei", "two", "drei", "trois");
        MapUtil.mergeIfAbsent(origMap, toMergeMap);
        assertEquals(expected, origMap);
    }

    @Test
    public void testPutIfAbsent() {
        Map<String, String> map = createMap("eins", "one");
        MapUtil.putIfAbsent(map, "eins", "un");
        assertEquals(1,map.size());
        assertEquals("one", map.get("eins"));
        MapUtil.putIfAbsent(map, "zwei", "deux");
        assertEquals(2, map.size());
        assertEquals("one", map.get("eins"));
        assertEquals("deux", map.get("zwei"));
    }

    @Test
    public void testMergeMaps() {
        Map<String, String> mapA = createMap("eins", "one", "zwei", "two");
        Map<String, String> mapB = createMap("zwei", "deux", "drei", "trois");
        Map<String, String> expectedA = createMap("eins", "one", "zwei", "two", "drei", "trois");
        Map<String, String> expectedB = createMap("eins", "one", "zwei", "deux", "drei", "trois");

        assertEquals(expectedA, MapUtil.mergeMaps(mapA, mapB));
        assertEquals(expectedB, MapUtil.mergeMaps(mapB, mapA));
    }

    @Test
    public void testGetFlattenedMap() {
        // Given
        final Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("one", "1");
        originalMap.put("two", Arrays.asList("1", "2", "3"));
        originalMap.put("three", Collections.singletonMap("three-nested", createMap("k1", "v1", "k2", "v2")));
        originalMap.put("four", Collections.singletonMap("four-nested", Arrays.asList(1, 2, 3, 4)));
        final Map<String, Object> fiveNested = new LinkedHashMap();
        originalMap.put("five", Collections.singletonMap("five-nested", fiveNested));
        fiveNested.put("k1", createMap("nk1", "nv1", "nk2", "nv2"));
        fiveNested.put("k2", Arrays.asList(true, false));
        originalMap.put("6", Collections.singletonMap("six-nested", Collections.singletonMap("k1", "v1")));
        originalMap.put("hit.the.corner", Collections.singletonMap("please", "you"));
        // When
        final Map<String, Object> result = getFlattenedMap(originalMap);
        // Then
        assertThat(result).containsExactly(
            entry("one", "1"),
            entry("two[0]", "1"),
            entry("two[1]", "2"),
            entry("two[2]", "3"),
            entry("three.three-nested.k1", "v1"),
            entry("three.three-nested.k2", "v2"),
            entry("four.four-nested[0]", "1"),
            entry("four.four-nested[1]", "2"),
            entry("four.four-nested[2]", "3"),
            entry("four.four-nested[3]", "4"),
            entry("five.five-nested.k1.nk1", "nv1"),
            entry("five.five-nested.k1.nk2", "nv2"),
            entry("five.five-nested.k2[0]", "true"),
            entry("five.five-nested.k2[1]", "false"),
            entry("6.six-nested.k1", "v1"),
            entry("hit.the.corner.please", "you")
        );
    }


    private Map<String, String> createMap(String ... args) {
        Map<String, String> ret = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i+=2) {
            ret.put(args[i], args[i+1]);
        }
        return ret;
    }
}
