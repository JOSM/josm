// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code Node} class.
 */
public class NodeTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Non-regression test for ticket #12060.
     */
    @Test
    public void testTicket12060() {
        DataSet ds = new DataSet();
        ds.addDataSource(new DataSource(new Bounds(LatLon.ZERO), null));
        Node n = new Node(1, 1);
        n.setCoor(LatLon.ZERO);
        ds.addPrimitive(n);
        n.setCoor(null);
        assertFalse(n.isNewOrUndeleted());
        assertNotNull(ds.getDataSourceArea());
        assertNull(n.getCoor());
        assertFalse(n.isOutsideDownloadArea());
    }

    /**
     * Test BBox calculation with Node
     */
    @Test
    public void testBBox() {
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

        assertFalse(n1.getBBox().isValid());
        assertTrue(n2.getBBox().isValid());
        assertTrue(n3.getBBox().isValid());
        assertTrue(n4.getBBox().isValid());
        BBox box1 = n1.getBBox();
        box1.add(n2.getCoor());
        assertTrue(box1.isValid());
        BBox box2 = n2.getBBox();
        box2.add(n1.getCoor());
        assertTrue(box2.isValid());
        assertEquals(box1, box2);
        box1.add(n3.getCoor());
        assertTrue(box1.isValid());
        assertEquals(box1.getCenter(), new LatLon(15, 15));
    }

    /**
     * Test that {@link Node#cloneFrom} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCloneFromIAE() {
        new Node().cloneFrom(new Way());
    }

    /**
     * Test that {@link Node#mergeFrom} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMergeFromIAE() {
        new Node().mergeFrom(new Way());
    }

    /**
     * Test that {@link Node#load} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLoadIAE() {
        new Node().load(new WayData());
    }

    /**
     * Test that {@link Node#isOutSideWorld} works as expected.
     */
    @Test
    public void testOutsideWorld() {
        Node n = new Node(1, 1);
        n.setCoor(LatLon.ZERO);
        assertFalse(n.isOutSideWorld());
        n.setCoor(null);
        assertFalse(n.isOutSideWorld());
        n.setCoor(LatLon.NORTH_POLE);
        assertTrue(n.isOutSideWorld());
        n.setCoor(new LatLon(0, 180.0));
        assertFalse(n.isOutSideWorld());
        // simulate a small move east
        n.setEastNorth(new EastNorth(n.getEastNorth().getX() + 0.1, n.getEastNorth().getY()));
        assertTrue(n.isOutSideWorld());
        n.setCoor(new LatLon(0, -180.0));
        assertFalse(n.isOutSideWorld());
        // simulate a small move west
        n.setEastNorth(new EastNorth(n.getEastNorth().getX() - 0.1, n.getEastNorth().getY()));
        assertTrue(n.isOutSideWorld());
    }

}
