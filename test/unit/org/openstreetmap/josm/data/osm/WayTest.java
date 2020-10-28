// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests of the {@code Way} class.
 * @since 11270
 */
class WayTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test BBox calculation with Way
     */
    @Test
    void testBBox() {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.setIncomplete(true);
        n2.setCoor(new LatLon(10, 10));
        n3.setCoor(new LatLon(20, 20));
        n4.setCoor(new LatLon(90, 180));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(n4);
        Way way = new Way(1);
        assertFalse(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n1));
        assertFalse(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n2));
        assertTrue(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n1, n2));
        assertTrue(way.getBBox().isValid());
        assertEquals(way.getBBox(), new BBox(10, 10));
    }

    /**
     * Test remove node
     */
    @Test
    void testRemoveNode() {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.setCoor(new LatLon(10, 10));
        n2.setCoor(new LatLon(11, 11));
        n3.setCoor(new LatLon(12, 12));
        n4.setCoor(new LatLon(13, 13));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(n4);
        Way way = new Way(1);
        ds.addPrimitive(way);
        // duplicated way node
        way.setNodes(Arrays.asList(n1, n2, n2, n3, n4, n1));
        way.setIncomplete(false);
        way.removeNode(n4);
        assertEquals(Arrays.asList(n1, n2, n3, n1), way.getNodes());
        way.removeNode(n3);
        assertEquals(Arrays.asList(n1, n2), way.getNodes());
        way.setNodes(Arrays.asList(n1, n2, n3, n4, n1));
        way.removeNode(n1);
        assertEquals(Arrays.asList(n2, n3, n4, n2), way.getNodes());
    }

    /**
     * Test remove node
     */
    @Test
    void testRemoveNodes() {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.setCoor(new LatLon(10, 10));
        n2.setCoor(new LatLon(11, 11));
        n3.setCoor(new LatLon(12, 12));
        n4.setCoor(new LatLon(13, 13));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(n4);
        Way way = new Way(1);
        ds.addPrimitive(way);
        // duplicated way node
        way.setNodes(Arrays.asList(n1, n2, n2, n3, n4, n1));
        way.setIncomplete(false);
        way.removeNodes(new HashSet<>(Arrays.asList(n3, n4)));
        assertEquals(Arrays.asList(n1, n2, n1), way.getNodes());
        way.setNodes(Arrays.asList(n1, n2, n3, n4, n1));
        way.removeNodes(new HashSet<>(Arrays.asList(n1)));
        assertEquals(Arrays.asList(n2, n3, n4, n2), way.getNodes());
    }

    /**
     * Test that {@link Way#cloneFrom} throws IAE for invalid arguments
     */
    @Test
    void testCloneFromIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Way().cloneFrom(new Node()));
    }

    /**
     * Test that {@link Way#load} throws IAE for invalid arguments
     */
    @Test
    void testLoadIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Way().load(new NodeData()));
    }
}
