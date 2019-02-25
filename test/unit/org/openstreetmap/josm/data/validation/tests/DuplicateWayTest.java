// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Duplicate way" validation test.
 */
public class DuplicateWayTest {

    /**
     * Setup test by initializing JOSM preferences and projection.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static final DuplicateWay TEST = new DuplicateWay();

    private static void doTest(int code) {
        doTest(code, "");
    }

    private static void doTest(int code, String tags) {
        doTest(code, tags, tags, true);
    }

    private static void doTest(int code, String tags1, String tags2, boolean fixable) {
        performTest(code, buildDataSet(tags1, tags2), fixable);
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

    private static DataSet buildDataSet(String tags1, String tags2) {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 6.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);
        ds.addPrimitive(TestUtils.newWay(tags1, a, b));
        ds.addPrimitive(TestUtils.newWay(tags2, a, b));
        return ds;
    }

    /**
     * Test of "Duplicate way" validation test - no tags.
     */
    @Test
    public void testDuplicateWayNoTags() {
        doTest(DuplicateWay.DUPLICATE_WAY);
    }

    /**
     * Test of "Duplicate way" validation test - same tags.
     */
    @Test
    public void testDuplicateWaySameTags() {
        doTest(DuplicateWay.DUPLICATE_WAY, "highway=motorway");
    }

    /**
     * Test of "Duplicate way" validation test - different tags.
     */
    @Test
    public void testDuplicateWayDifferentTags() {
        doTest(DuplicateWay.SAME_WAY, "highway=motorway", "highway=trunk", false);
    }
}
