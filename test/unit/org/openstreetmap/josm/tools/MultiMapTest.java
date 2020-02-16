// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz")), map.getValues("foo"));
        assertEquals(new HashSet<>(), map.get("alpha"));
        assertEquals(Collections.emptySet(), map.getValues("alpha"));
        assertNull(map.get("beta"));
        assertEquals(Collections.emptySet(), map.getValues("beta"));
        assertEquals(new HashSet<>(), map.getValues("alpha"));
        assertEquals(new HashSet<>(), map.getValues("beta"));
        map.put("foo", "baz2");
        map.put("foo", "baz");
        assertTrue(map.containsKey("foo"));
        assertTrue(map.contains("foo", "bar"));
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz", "baz2")), map.get("foo"));
        assertFalse(map.contains("foo", "xxx"));
        assertFalse(map.remove("foo", "xxx"));
        assertTrue(map.remove("foo", "baz"));
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz2")), map.get("foo"));
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz2")), map.remove("foo"));
        assertFalse(map.containsKey("foo"));
        assertNull(map.get("foo"));
        assertEquals("(alpha->[])", map.toString());
        assertEquals(Collections.emptySet(), map.remove("alpha"));
        assertTrue(map.isEmpty());
        assertFalse(map.remove("omega", null));
        assertTrue(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        map.putAll("foo", Arrays.asList("bar", "baz"));
        assertEquals(new HashSet<>(Arrays.asList("bar", "baz")), map.get("foo"));
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, new MultiMap<String, String>(null).size());
        final Map<String, Set<String>> asMap = Collections.singletonMap("foo", Collections.singleton("bar"));
        assertEquals(asMap, new MultiMap<>(asMap).toMap());
        assertEquals("[foo=[bar]]", new MultiMap<>(asMap).entrySet().toString());
        assertEquals("[foo]", new MultiMap<>(asMap).keySet().toString());
        assertEquals("[[bar]]", new MultiMap<>(asMap).values().toString());
    }
}
