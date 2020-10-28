// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Some unit test cases for basic tag management on {@link OsmPrimitive}. Uses
 * {@link Node} for the tests, {@link OsmPrimitive} is abstract.
 */
class OsmPrimitiveKeyHandlingTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * test query and get methods on a node withouth keys
     */
    @Test
    void testEmptyNode() {
        Node n = new Node();
        testKeysSize(n, 0);
        testGetKey(n, "nosuchkey", null);

        n.remove("nosuchkey"); // should work
        testKeysSize(n, 0);
        testGetKey(n, "nosuchkey", null);
    }

    /**
     * Adds a tag to an empty node and test the query and get methods.
     */
    @Test
    void testPut() {
        Node n = new Node();
        n.put("akey", "avalue");
        testKeysSize(n, 1);

        testGetKey(n, "akey", "avalue");
    }

    /**
     * Adds two tags to an empty node and test the query and get methods.
     */
    @Test
    void testPut2() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put("key.2", "value.2");
        assertTrue(n.get("key.1").equals("value.1"));
        assertTrue(n.get("key.2").equals("value.2"));
        testKeysSize(n, 2);
        assertTrue(n.hasKeys());
        assertTrue(n.hasKey("key.1"));
        assertTrue(n.hasKey("key.2"));
        assertFalse(n.hasKey("nosuchkey"));
    }

    /**
     * Removes tags from a node with two tags and test the state of the node.
     */
    @Test
    @SuppressFBWarnings(value = "DM_STRING_CTOR", justification = "test that equals is used and not ==")
    void testRemove() {
        Node n = new Node();
        n.put("key.1", "value.1");
        n.put(new String("key.2"), new String("value.2")); // Test that equals is used and not ==

        testGetKey(n, "key.1", "value.1");
        testGetKey(n, "key.2", "value.2");

        n.remove("nosuchkey");             // should work
        testKeysSize(n, 2);                // still 2 tags ?

        testGetKey(n, "key.1", "value.1");
        testGetKey(n, "key.2", "value.2");

        n.remove("key.1");
        testKeysSize(n, 1);
        assertTrue(n.hasKeys());

        testGetKey(n, "key.1", null);
        testGetKey(n, "key.2", "value.2");

        n.remove("key.2");
        testKeysSize(n, 0);
        assertFalse(n.hasKeys());
        testGetKey(n, "key.1", null);
        testGetKey(n, "key.2", null);
    }

    /**
     * Removes all tags from a node.
     */
    @Test
    void testRemoveAll() {
        Node n = new Node();

        n.put("key.1", "value.1");
        n.put("key.2", "value.2");

        n.removeAll();
        testKeysSize(n, 0);
    }

    /**
     * Test hasEqualSemanticAttributes on two nodes whose identical tags are added
     * in different orders.
     */
    @Test
    void testHasEqualSemanticAttributes() {
        Node n1 = new Node(1);
        n1.setCoor(LatLon.ZERO);
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.2");

        Node n2 = new Node(1);
        n2.setCoor(LatLon.ZERO);
        n2.put("key.2", "value.2");
        n2.put("key.1", "value.1");

        assertTrue(n1.hasEqualSemanticAttributes(n2));
    }

    /**
     * Test hasEqualSemanticAttributes on two nodes with different tags.
     */
    @Test
    void testHasEqualSemanticAttributes2() {
        Node n1 = new Node(1);
        n1.setCoor(LatLon.ZERO);
        n1.put("key.1", "value.1");
        n1.put("key.2", "value.3");

        Node n2 = new Node(1);
        n2.setCoor(LatLon.ZERO);
        n2.put("key.1", "value.1");
        n2.put("key.2", "value.4");

        assertFalse(n1.hasEqualSemanticAttributes(n2));
    }

    /**
     * Tests if the size of the keys map is right.
     * @author Michael Zangl
     * @param p The primitive (node) to test
     * @param expectedSize The expected size.
     * @throws AssertionError on failure.
     */
    private void testKeysSize(OsmPrimitive p, int expectedSize) {
        assertEquals(expectedSize, p.getKeys().size());
        assertEquals(expectedSize, p.keySet().size());
        assertEquals(expectedSize, p.getKeys().entrySet().size());
        assertEquals(expectedSize, p.getKeys().keySet().size());
        assertEquals(expectedSize, p.getNumKeys());
        boolean empty = expectedSize == 0;
        assertEquals(empty, p.getKeys().isEmpty());
        assertEquals(empty, p.keySet().isEmpty());
        assertEquals(empty, p.getKeys().entrySet().isEmpty());
        assertEquals(empty, p.getKeys().keySet().isEmpty());
        assertEquals(!empty, p.hasKeys());
    }

    /**
     * Tests all key get methods for that node.
     * @author Michael Zangl
     * @param p The primitive (node)
     * @param key The key to test
     * @param value The value the key should have.
     * @throws AssertionError on failure.
     */
    private void testGetKey(OsmPrimitive p, String key, String value) {
        assertEquals(value != null, p.hasKey(key));
        assertEquals(value != null, p.getKeys().containsKey(key));
        assertEquals(value != null, p.getKeys().keySet().contains(key));
        assertEquals(value, p.get(key));
        assertEquals(value, p.getKeys().get(key));
    }

}
