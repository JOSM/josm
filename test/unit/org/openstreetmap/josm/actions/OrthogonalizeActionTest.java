// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static junit.framework.Assert.assertEquals;

import java.io.FileInputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for class {@link OsmDataLayer}.
 */
public class OrthogonalizeActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    @Test(expected = OrthogonalizeAction.InvalidUserInputException.class)
    public void testNoSelection() throws Exception {
        performTest("nothing selected");
    }

    @Test
    public void testClosedWay() throws Exception {
        final DataSet ds = performTest("name=ClosedWay");
        final Way way = ds.getSelectedWays().iterator().next();
        assertEquals(new LatLon(8.538808176881814, 55.72978898396922), way.getNode(0).getCoor());
        assertEquals(new LatLon(8.539618224318104, 55.73039799489563), way.getNode(1).getCoor());
        assertEquals(new LatLon(8.538993302766201, 55.73124794515577), way.getNode(2).getCoor());
        assertEquals(new LatLon(8.538183254003354, 55.730638934229376), way.getNode(3).getCoor());
        verifyRectangleClockwise(way);
    }

    @Test
    public void testTwoWaysFormingClosedWay() throws Exception {
        performTest("name=TwoWaysFormingClosedWay");
    }

    @Test
    public void testTwoRingsAtOnce() throws Exception {
        performTest("name=ClosedWay OR name=TwoWaysFormingClosedWay");
    }

    @Test
    public void testClosedWayWithReferenceNodes() throws Exception {
        final DataSet ds = performTest("name=ClosedWayWithReferenceNodes");
        final Way way = ds.getSelectedWays().iterator().next();
        assertEquals(new LatLon(8.534711427, 55.73000670312), way.getNode(0).getCoor());
        assertEquals(new LatLon(8.53547720918594, 55.73067141759374), way.getNode(1).getCoor());
        assertEquals(new LatLon(8.534835495633061, 55.73142735279376), way.getNode(2).getCoor());
        assertEquals(new LatLon(8.53406971216, 55.73076263832), way.getNode(3).getCoor());
        verifyRectangleClockwise(way);
    }

    DataSet performTest(String search) throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "orthogonalize.osm")) {
            final DataSet ds = OsmReader.parseDataSet(in, null);
            ds.setSelected(Utils.filter(ds.allPrimitives(), SearchCompiler.compile(search)));
            OrthogonalizeAction.orthogonalize(ds.getSelected()).executeCommand();
            return ds;
        }
    }

    void verifyRectangleClockwise(final Way way) {
        for (int i = 1; i < way.getNodesCount() - 1; i++) {
            assertEquals(-Math.PI / 2,  Geometry.getCornerAngle(
                    way.getNode(i - 1).getEastNorth(), way.getNode(i).getEastNorth(), way.getNode(i + 1).getEastNorth()), 1e-6);
        }
    }
}
