// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link MultiMap} class.
 */
public class MultiMapTest {

    /**
     * Unit test of methods {@link MultiMap#equals} and {@link MultiMap#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(MultiMap.class).usingGetClass().verify();
    }

    /**
     * Various test of {@link MultiMap}.
     */
    @Test
    public void testMultiMap() {
        final MultiMap<String, String> map = new MultiMap<>();
        assertTrue(map.isEmpty());
        map.put("foo", "bar");
        map.put("foo", "baz");
        map.putVoid("alpha");
        assertEquals(2, map.size());
        assertEquals(new HashSet<>(Arrays.asList("foo", "alpha")), map.keySet());
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz")), map.get("foo"));
        assertEquals(new HashSet<>(), map.get("alpha"));
        assertEquals(null, map.get("beta"));
        assertEquals(new HashSet<>(), map.getValues("alpha"));
        assertEquals(new HashSet<>(), map.getValues("beta"));
        map.put("foo", "baz2");
        map.put("foo", "baz");
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz", "baz2")), map.get("foo"));
        map.remove("foo", "baz");
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz2")), map.get("foo"));
        map.remove("foo");
        assertEquals(null, map.get("foo"));
        assertEquals("(alpha->[])", map.toString());
        map.remove("alpha");
        assertTrue(map.isEmpty());
        map.remove("omega", null);
        assertTrue(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        map.putAll("foo", Arrays.asList("bar", "baz"));
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz")), map.get("foo"));
    }
}
