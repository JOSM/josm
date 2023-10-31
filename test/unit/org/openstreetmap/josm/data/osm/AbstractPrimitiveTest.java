// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of the {@code AbstractPrimitive} class.
 */
class AbstractPrimitiveTest {
    /**
     * Unit test of {@link AbstractPrimitive#isUndeleted} method.
     */
    @Test
    void testIsUndeleted() {
        AbstractPrimitive p = new Node(1);
        p.setVisible(false);
        p.setDeleted(false);
        assertFalse(p.isVisible());
        assertFalse(p.isDeleted());
        assertTrue(p.isUndeleted());

        p.setVisible(false);
        p.setDeleted(true);
        assertFalse(p.isVisible());
        assertTrue(p.isDeleted());
        assertFalse(p.isUndeleted());

        p.setVisible(true);
        p.setDeleted(false);
        assertTrue(p.isVisible());
        assertFalse(p.isDeleted());
        assertFalse(p.isUndeleted());

        p.setVisible(true);
        p.setDeleted(true);
        assertTrue(p.isVisible());
        assertTrue(p.isDeleted());
        assertFalse(p.isUndeleted());
    }

    /**
     * Unit test of {@link AbstractPrimitive#hasTagDifferent} methods.
     */
    @Test
    void testHasTagDifferent() {
        AbstractPrimitive p = new Node();

        assertFalse(p.hasTagDifferent("foo", "bar"));
        assertFalse(p.hasTagDifferent("foo", "bar", "baz"));
        assertFalse(p.hasTagDifferent("foo", Collections.singleton("bar")));

        p.put("foo", "bar");
        assertTrue(p.hasTagDifferent("foo", "baz"));
        assertFalse(p.hasTagDifferent("foo", "bar"));
        assertFalse(p.hasTagDifferent("foo", "bar", "baz"));
        assertFalse(p.hasTagDifferent("foo", Collections.singleton("bar")));

        p.put("foo", "foo");
        assertTrue(p.hasTagDifferent("foo", "bar"));
        assertTrue(p.hasTagDifferent("foo", "bar", "baz"));
        assertTrue(p.hasTagDifferent("foo", Collections.singleton("bar")));
    }

    /**
     * Unit test of {@link AbstractPrimitive#putAll}
     */
    @Test
    void testPutAllInsert() {
        AbstractPrimitive p = new Node();
        Map<String, String> tags = new HashMap<>();

        tags.put("a", "va1");
        tags.put("b", "vb1");
        p.putAll(tags);
        assertEquals("va1", p.get("a"));
        assertEquals("vb1", p.get("b"));

    }

    /**
     * Unit test of {@link AbstractPrimitive#putAll}
     */
    @Test
    void testPutAllChange() {
        AbstractPrimitive p = TestUtils.newNode("a=va1 b=vb1");
        Map<String, String> tags = new HashMap<>();

        tags.put("a", "va2");
        p.putAll(tags);
        assertEquals("va2", p.get("a"));
        assertEquals("vb1", p.get("b"));
    }

    /**
     * Unit test of {@link AbstractPrimitive#putAll}
     */
    @Test
    void testPutAllChangeAndInsert() {
        AbstractPrimitive p = TestUtils.newNode("a=va2 b=vb1");
        Map<String, String> tags = new HashMap<>();

        tags.put("b", "vb3");
        tags.put("c", "vc3");
        p.putAll(tags);
        assertEquals("va2", p.get("a"));
        assertEquals("vb3", p.get("b"));
        assertEquals("vc3", p.get("c"));
    }

    /**
     * Unit test of {@link AbstractPrimitive#putAll}
     */
    @Test
    void testPutAllChangeAndRemove() {
        AbstractPrimitive p = TestUtils.newNode("a=va2 b=vb3 c=vc3");
        Map<String, String> tags = new HashMap<>();

        tags.put("a", "va4");
        tags.put("b", null);
        tags.put(null, null);
        p.putAll(tags);
        assertEquals("va4", p.get("a"));
        assertNull(p.get("b"));
        assertEquals("vc3", p.get("c"));
    }
}
