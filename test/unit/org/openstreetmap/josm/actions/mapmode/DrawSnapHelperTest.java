// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JCheckBoxMenuItem;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test class for {@link DrawSnapHelper}
 */
@Main
@org.openstreetmap.josm.testutils.annotations.Projection
class DrawSnapHelperTest {
    static Stream<Arguments> testNonRegression13097() {
        return Stream.of(
                Arguments.of(Projections.getProjectionByCode("EPSG:4326")), // WGS84 Geographic
                Arguments.of(Projections.getProjectionByCode("EPSG:3857")) // Mercator
        );
    }

    /**
     * See #13097: Angle snapping impossible with WGS84 projection
     */
    @ParameterizedTest
    @MethodSource
    void testNonRegression13097(Projection projection) {
        ProjectionRegistry.setProjection(projection);
        DrawAction drawAction = new DrawAction();
        DrawSnapHelper drawSnapHelper = new DrawSnapHelper(drawAction);
        drawSnapHelper.setMenuCheckBox(new JCheckBoxMenuItem()); // Just needed to avoid an NPE in enableSnapping
        drawSnapHelper.init(); // Needed to get the default angle snaps
        drawSnapHelper.enableSnapping();
        Way way1 = new Way();
        Node node1 = new Node(new LatLon(39.1260035, -108.5624143));
        Node node2 = new Node(new LatLon(39.1260973, -108.5622908));
        way1.setNodes(Arrays.asList(node1, node2));
        DataSet ds = new DataSet();
        ds.addPrimitiveRecursive(way1);
        ds.setSelected(way1, node2);
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "testNonRegression13097", null));
        // We need to ensure that the mapView is zoomed in enough. This takes several zoom in events after we zoom to node2.
        MainApplication.getMap().mapView.zoomTo(node2);
        for (int i = 0; i < 4; i++) {
            MainApplication.getMap().mapView.zoomIn();
        }
        drawAction.enterMode();
        drawSnapHelper.setBaseSegment(new WaySegment(way1, 0));
        EastNorth position = projection.latlon2eastNorth(new LatLon(39.1260263, -108.5621895));
        double wayHeading = Utils.toDegrees(
                assertDoesNotThrow(() -> way1.firstNode().getEastNorth(projection).heading(way1.lastNode().getEastNorth()))
        );
        drawSnapHelper.checkAngleSnapping(position, wayHeading, wayHeading + 90);
        EastNorth snapPoint = drawSnapHelper.getSnapPoint(position);
        // We can't really check the latlon -- 90 degrees is different based off of projection.
        assertNotSame(position, snapPoint, "The snap point should not be the same as the original position");
        assertEquals(90, Math.abs(wayHeading - Utils.toDegrees(node2.getEastNorth(projection).heading(snapPoint))), 1e-8);
    }
}
