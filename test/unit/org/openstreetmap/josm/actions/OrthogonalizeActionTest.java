// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
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

    @Test
    public void testFourNodes() throws Exception {
        final DataSet ds = performTest(
                "name=NodeToRectify-01", "name=NodeToRectify-02", "name=NodeToRectify-03", "name=NodeToRectify-04");
        final List<Node> nodes = new ArrayList<>(ds.getSelectedNodes());
        assertEquals(new LatLon(8.532735415272217, 55.72986948949525), nodes.get(0).getCoor());
        assertEquals(new LatLon(8.533520827858515, 55.73043325105434), nodes.get(1).getCoor());
        assertEquals(new LatLon(8.532914283300173, 55.73129729115582), nodes.get(2).getCoor());
        assertEquals(new LatLon(8.532055019939826, 55.73068052126457), nodes.get(3).getCoor());
    }

    DataSet performTest(String... search) throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "orthogonalize.osm")) {
            final DataSet ds = OsmReader.parseDataSet(in, null);
            for (String s : search) {
                ds.addSelected(Utils.filter(ds.allPrimitives(), SearchCompiler.compile(s)));
            }
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
