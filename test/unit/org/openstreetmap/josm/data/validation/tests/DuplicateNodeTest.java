// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
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
