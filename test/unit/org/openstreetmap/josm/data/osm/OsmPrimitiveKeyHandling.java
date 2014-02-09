// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Some unit test cases for basic tag management on {@link OsmPrimitive}. Uses
 * {@link Node} for the tests, {@link OsmPrimitive} is abstract.
 *
 */
public class OsmPrimitiveKeyHandling {

    @BeforeClass
    public static void init() {
        Main.initApplicationPreferences();
    }

    /**
     * test query and get methods on a node withouth keys
     */
    @Test
    public void emptyNode() {
        Node n = new Node();
        assertTrue(n.getKeys().size() == 0);
        assertTrue(!n.hasKeys());
        assertTrue(!n.hasKey("nosuchkey"));
        assertTrue(n.keySet().isEmpty());

        n.remove("nosuchkey"); // should work
    }

    /**
     * Add a tag to an empty node and test the query and get methods.
     *
     */
    @Test
    public void put() {
        Node n = new Node();
        n.put("akey", "avalue");
        assertTrue(n.get("akey").equals("avalue"));
        assertTrue(n.getKeys().size() == 1);

        assertTrue(n.keySet().size() == 1);
        assertTrue(n.keySet().contains("akey"));
    }

    /**
     * Add two tags to an empty node and test the query and get methods.
     */
    @Test
    public void put2() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put("key.2", "value.2");
        assertTrue(n.get("key.1").equals("value.1"));
        assertTrue(n.get("key.2").equals("value.2"));
        assertTrue(n.getKeys().size() == 2);
        assertTrue(n.hasKeys());
        assertTrue(n.hasKey("key.1"));
        assertTrue(n.hasKey("key.2"));
        assertTrue(!n.hasKey("nosuchkey"));
    }

    /**
     * Remove tags from a node with two tags and test the state of the node.
     *
     */
    @Test
    public void remove() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put("key.2", "value.2");

        n.remove("nosuchkey");               // should work
        assertTrue(n.getKeys().size() == 2); // still 2 tags ?

        n.remove("key.1");
        assertTrue(n.getKeys().size() == 1);
        assertTrue(!n.hasKey("key.1"));
        assertTrue(n.get("key.1") == null);
        assertTrue(n.hasKey("key.2"));
        assertTrue(n.get("key.2").equals("value.2"));

        n.remove("key.2");
        assertTrue(n.getKeys().size() == 0);
        assertTrue(!n.hasKey("key.1"));
        assertTrue(n.get("key.1") == null);
        assertTrue(!n.hasKey("key.2"));
        assertTrue(n.get("key.2") == null);
    }

    /**
     * Remove all tags from a node
     *
     */
    @Test
    public void removeAll() {
        Node n = new Node();

        n.put("key.1", "value.1");
        n.put("key.2", "value.2");

        n.removeAll();
        assertTrue(n.getKeys().size() == 0);
    }

    /**
     * Test hasEqualSemanticAttributes on two nodes whose identical tags are added
     * in different orders.
     */
    @Test
    public void hasEqualSemanticAttributes() {
        Node n1 = new Node(1);
        n1.setCoor(new LatLon(0,0));
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.2");

        Node n2 = new Node(1);
        n2.setCoor(new LatLon(0,0));
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
        n1.setCoor(new LatLon(0,0));
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.3");

        Node n2 = new Node(1);
        n2.setCoor(new LatLon(0,0));
        n2.put("key.1", "value.1");
        n2.put("key.2", "value.4");

        assertTrue(!n1.hasEqualSemanticAttributes(n2));
    }

}
