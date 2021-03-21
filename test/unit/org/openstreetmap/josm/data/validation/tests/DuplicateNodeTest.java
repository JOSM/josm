// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Duplicate node" validation test.
 */
class DuplicateNodeTest {

    /**
     * Setup test by initializing JOSM preferences and projection.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static final DuplicateNode TEST = new DuplicateNode();

    private static void doTest(int code, Tag... tags) {
        performTest(code, buildDataSet(tags), true);
    }

    private static void performTest(int code, DataSet ds, boolean fixable) {
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(1, TEST.getErrors().size());
        TestError error = TEST.getErrors().iterator().next();
        assertEquals(code, error.getCode());
        assertEquals(fixable, error.isFixable());
        if (fixable) {
            Command c = error.getFix();
            assertNotNull(c);
            c.executeCommand();
            assertFalse(error.isFixable());
            c.undoCommand();
            assertTrue(error.isFixable());
            error.getPrimitives().iterator().next().setDeleted(true);
            if (error.getPrimitives().size() == 2) {
                assertFalse(error.isFixable());
            } else {
                assertTrue(error.isFixable());
            }
            error.getPrimitives().iterator().next().setDeleted(false);
        }
    }

    private static DataSet buildDataSet(Tag... tags) {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        if (tags.length > 0) {
            Way parent = new Way();
            parent.addNode(a);
            parent.addNode(b);
            for (Tag tag : tags) {
                parent.put(tag);
            }
            ds.addPrimitive(parent);
        }
        return ds;
    }

    /**
     * Test of "Duplicate node" validation test - no duplicate
     */
    @Test
    void testNoDuplicateNode() {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 6.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        a.put("foo", "bar");
        b.put("bar", "foo");

        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(0, TEST.getErrors().size());
    }

    /**
     * Test of "Duplicate node" validation test - same position, with ele value
     */
    @Test
    void testDuplicateNodeWithEle() {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        a.put("foo", "bar");
        b.put("bar", "foo");
        a.put("ele", "100");
        b.put("ele", "100");

        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(1, TEST.getErrors().size());

        b.put("ele", "110");

        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(0, TEST.getErrors().size());
    }

    /**
     * Test of "Duplicate node" validation test - three nodes
     */
    @Test
    void testDuplicateNodeTriple() {
        DataSet ds = new DataSet();
    
        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 5.0));
        Node c = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(c);
    
        performTest(DuplicateNode.DUPLICATE_NODE_OTHER, ds, true);
        a.put("foo", "bar");
        b.put("foo", "bar");
        performTest(DuplicateNode.DUPLICATE_NODE_OTHER, ds, true);
    }

    /**
     * Test of "Duplicate node" validation test - different tag sets.
     */
    @Test
    void testDuplicateNode() {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        a.put("foo", "bar");
        b.put("bar", "foo");

        performTest(DuplicateNode.DUPLICATE_NODE, ds, false);
    }

    /**
     * Test of "Duplicate node" validation test - server precision.
     *
     * Non-regression test for ticket #18074.
     */
    @Test
    void testServerPrecision() {
        DuplicateNode.NodeHash nodeHash = new DuplicateNode.NodeHash();
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(-23.51108285, -46.489264256));
        Node b = new Node(new LatLon(-23.511082861, -46.489264251));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        a.put("foo", "bar");
        b.put("bar", "foo");

        // on OSM server, both are: lat = -23.5110829 lon = -46.4892643
        assertEquals(new LatLon(-23.5110828, -46.4892643), a.getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(-23.5110829, -46.4892643), b.getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(-23.511083, -46.489264), nodeHash.roundCoord(a.getCoor()));
        assertEquals(new LatLon(-23.511083, -46.489264), nodeHash.roundCoord(b.getCoor()));
        performTest(DuplicateNode.DUPLICATE_NODE, ds, false);
    }

    /**
     * Test of "Duplicate node" validation test - mixed case.
     */
    @Test
    void testDuplicateNodeMixed() {
        doTest(DuplicateNode.DUPLICATE_NODE_MIXED, new Tag("building", "foo"), new Tag("highway", "bar"));
    }

    /**
     * Test of "Duplicate node" validation test - other case.
     */
    @Test
    void testDuplicateNodeOther() {
        doTest(DuplicateNode.DUPLICATE_NODE_OTHER);
    }

    /**
     * Test of "Duplicate node" validation test - building case.
     */
    @Test
    void testDuplicateNodeBuilding() {
        doTest(DuplicateNode.DUPLICATE_NODE_BUILDING, new Tag("building", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - boundary case.
     */
    @Test
    void testDuplicateNodeBoundary() {
        doTest(DuplicateNode.DUPLICATE_NODE_BOUNDARY, new Tag("boundary", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - highway case.
     */
    @Test
    void testDuplicateNodeHighway() {
        doTest(DuplicateNode.DUPLICATE_NODE_HIGHWAY, new Tag("highway", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - landuse case.
     */
    @Test
    void testDuplicateNodeLanduse() {
        doTest(DuplicateNode.DUPLICATE_NODE_LANDUSE, new Tag("landuse", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - natural case.
     */
    @Test
    void testDuplicateNodeNatural() {
        doTest(DuplicateNode.DUPLICATE_NODE_NATURAL, new Tag("natural", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - power case.
     */
    @Test
    void testDuplicateNodePower() {
        doTest(DuplicateNode.DUPLICATE_NODE_POWER, new Tag("power", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - railway case.
     */
    @Test
    void testDuplicateNodeRailway() {
        doTest(DuplicateNode.DUPLICATE_NODE_RAILWAY, new Tag("railway", "foo"));
    }

    /**
     * Test of "Duplicate node" validation test - waterway case.
     */
    @Test
    void testDuplicateNodeWaterway() {
        doTest(DuplicateNode.DUPLICATE_NODE_WATERWAY, new Tag("waterway", "foo"));
    }
}
