// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.OrthogonalizeAction.Direction;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests for class {@link OrthogonalizeAction}.
 */
public class OrthogonalizeActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    @Test(expected = OrthogonalizeAction.InvalidUserInputException.class)
    public void testNoSelection() throws Exception {
        performTest("nothing selected");
    }

    @Test
    public void testClosedWay() throws Exception {
        final DataSet ds = performTest("name=ClosedWay");
        final Way way = ds.getSelectedWays().iterator().next();
        assertEquals(new LatLon(8.5388082, 55.7297890), way.getNode(0).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5396182, 55.7303980), way.getNode(1).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5389933, 55.7312479), way.getNode(2).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5381833, 55.7306389), way.getNode(3).getCoor().getRoundedToOsmPrecision());
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
        assertEquals(new LatLon(8.5347114, 55.7300067), way.getNode(0).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5354772, 55.7306714), way.getNode(1).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5348355, 55.7314274), way.getNode(2).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5340697, 55.7307626), way.getNode(3).getCoor().getRoundedToOsmPrecision());
        verifyRectangleClockwise(way);
    }

    @Test
    public void testFourNodes() throws Exception {
        final DataSet ds = performTest(
                "name=NodeToRectify-01", "name=NodeToRectify-02", "name=NodeToRectify-03", "name=NodeToRectify-04");
        final List<Node> nodes = new ArrayList<>(ds.getSelectedNodes());
        assertEquals(new LatLon(8.5327354, 55.7298695), nodes.get(0).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5335208, 55.7304333), nodes.get(1).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5329143, 55.7312973), nodes.get(2).getCoor().getRoundedToOsmPrecision());
        assertEquals(new LatLon(8.5320550, 55.7306805), nodes.get(3).getCoor().getRoundedToOsmPrecision());
    }

    /**
     * Tests that {@code OrthogonalizeAction.EN} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(OrthogonalizeAction.EN.class);
    }

    DataSet performTest(String... search) throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "orthogonalize.osm")) {
            final DataSet ds = OsmReader.parseDataSet(in, null);
            // TODO: Executing commands depends on active edit layer
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "ds", null));
            for (String s : search) {
                ds.addSelected(SubclassFilteredCollection.filter(ds.allPrimitives(), SearchCompiler.compile(s)));
            }
            OrthogonalizeAction.orthogonalize(ds.getSelected()).executeCommand();
            return ds;
        }
    }

    void verifyRectangleClockwise(final Way way) {
        for (int i = 1; i < way.getNodesCount() - 1; i++) {
            assertEquals(-Math.PI / 2, Geometry.getCornerAngle(
                    way.getNode(i - 1).getEastNorth(), way.getNode(i).getEastNorth(), way.getNode(i + 1).getEastNorth()), 1e-6);
        }
    }

    /**
     * Unit test of {@link Direction} enum.
     */
    @Test
    public void testEnumDirection() {
        TestUtils.superficialEnumCodeCoverage(Direction.class);
    }
}
