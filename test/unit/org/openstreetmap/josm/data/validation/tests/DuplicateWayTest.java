// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;

/**
 * JUnit Test of "Duplicate way" validation test.
 */
class DuplicateWayTest {
    private static final DuplicateWay TEST = new DuplicateWay();

    private static void doTest(int code) {
        doTest(code, "");
    }

    private static void doTest(int code, String tags) {
        doTest(code, tags, tags, true);
    }

    private static void doTest(int code, String tags1, String tags2, boolean fixable) {
        performTest(code, buildDataSet(tags1, tags2), fixable);
        performPartialTest(code, buildDataSet(tags1, tags2), fixable);
    }

    private static void performTest(int code, DataSet ds, boolean fixable) {
        TEST.setPartialSelection(false);
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(1, TEST.getErrors().size());
        TestError error = TEST.getErrors().iterator().next();
        assertEquals(code, error.getCode());
        assertEquals(fixable, error.isFixable());
    }

    private static void performPartialTest(int code, DataSet ds, boolean fixable) {
        ds.setSelected(ds.getWays().iterator().next());
        TEST.setPartialSelection(true);
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.getSelectedWays().iterator().next());
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
    void testDuplicateWayNoTags() {
        doTest(DuplicateWay.DUPLICATE_WAY);
    }

    /**
     * Test of "Duplicate way" validation test - same tags.
     */
    @Test
    void testDuplicateWaySameTags() {
        doTest(DuplicateWay.DUPLICATE_WAY, "highway=motorway");
    }

    /**
     * Test of "Duplicate way" validation test - different tags.
     */
    @Test
    void testDuplicateWayDifferentTags() {
        doTest(DuplicateWay.SAME_WAY, "highway=motorway", "highway=trunk", false);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14891">Bug #14891</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testFixError() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "duplicate-ways.osm")), null);
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();

        assertEquals(2, TEST.getErrors().size());
        for (TestError error: TEST.getErrors()) {
            error = TEST.getErrors().iterator().next();
            assertTrue(error.isFixable());
            Command fix = error.getFix();
            assertNotNull(fix);
        }
        for (TestError error: TEST.getErrors()) {
            error.getFix().executeCommand();
        }
        TEST.startTest(NullProgressMonitor.INSTANCE);
        TEST.visit(ds.allPrimitives());
        TEST.endTest();
        assertTrue(TEST.getErrors().isEmpty());

    }

}
