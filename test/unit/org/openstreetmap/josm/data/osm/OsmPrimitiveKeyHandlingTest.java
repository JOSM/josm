// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Some unit test cases for basic tag management on {@link OsmPrimitive}. Uses
 * {@link Node} for the tests, {@link OsmPrimitive} is abstract.
 */
public class OsmPrimitiveKeyHandlingTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * test query and get methods on a node withouth keys
     */
    @Test
    public void emptyNode() {
        Node n = new Node();
        assertSame(n.getKeys().size(), 0);
        assertFalse(n.hasKeys());
        assertFalse(n.hasKey("nosuchkey"));
        assertTrue(n.keySet().isEmpty());

        n.remove("nosuchkey"); // should work
    }

    /**
     * Adds a tag to an empty node and test the query and get methods.
     */
    @Test
    public void put() {
        Node n = new Node();
        n.put("akey", "avalue");
        assertTrue(n.get("akey").equals("avalue"));
        assertSame(n.getKeys().size(), 1);

        assertSame(n.keySet().size(), 1);
        assertTrue(n.keySet().contains("akey"));
    }

    /**
     * Adds two tags to an empty node and test the query and get methods.
     */
    @Test
    public void put2() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put("key.2", "value.2");
        assertTrue(n.get("key.1").equals("value.1"));
        assertTrue(n.get("key.2").equals("value.2"));
        assertSame(n.getKeys().size(), 2);
        assertTrue(n.hasKeys());
        assertTrue(n.hasKey("key.1"));
        assertTrue(n.hasKey("key.2"));
        assertFalse(n.hasKey("nosuchkey"));
    }

    /**
     * Removes tags from a node with two tags and test the state of the node.
     */
    @Test
    public void remove() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put("key.2", "value.2");

        n.remove("nosuchkey");             // should work
        assertSame(n.getKeys().size(), 2); // still 2 tags ?

        n.remove("key.1");
        assertSame(n.getKeys().size(), 1);
        assertFalse(n.hasKey("key.1"));
        assertNull(n.get("key.1"));
        assertTrue(n.hasKey("key.2"));
        assertTrue(n.get("key.2").equals("value.2"));

        n.remove("key.2");
        assertSame(n.getKeys().size(), 0);
        assertFalse(n.hasKey("key.1"));
        assertNull(n.get("key.1"));
        assertFalse(n.hasKey("key.2"));
        assertNull(n.get("key.2"));
    }

    /**
     * Removes all tags from a node.
     */
    @Test
    public void removeAll() {
        Node n = new Node();

        n.put("key.1", "value.1");
        n.put("key.2", "value.2");

        n.removeAll();
        assertSame(n.getKeys().size(), 0);
    }

    /**
     * Test hasEqualSemanticAttributes on two nodes whose identical tags are added
     * in different orders.
     */
    @Test
    public void hasEqualSemanticAttributes() {
        Node n1 = new Node(1);
        n1.setCoor(new LatLon(0, 0));
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.2");

        Node n2 = new Node(1);
        n2.setCoor(new LatLon(0, 0));
        n2.put("key.2", "value.2");
        n2.put("key.1", "value.1");

        assertTrue(n1.hasEqualSemanticAttributes(n2));
    }

    /**
     * Test hasEqualSemanticAttributes on two nodes with different tags.
     */
    @Test
    public void hasEqualSemanticAttributes_2() {
        Node n1 = new Node(1);
        n1.setCoor(new LatLon(0, 0));
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.3");

        Node n2 = new Node(1);
        n2.setCoor(new LatLon(0, 0));
        n2.put("key.1", "value.1");
        n2.put("key.2", "value.4");

        assertFalse(n1.hasEqualSemanticAttributes(n2));
    }
}
