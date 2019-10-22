package io.jkube.kit.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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


    private Map<String,String> createMap(String ... args) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < args.length; i+=2) {
            ret.put(args[i], args[i+1]);
        }
        return ret;
    }
}
