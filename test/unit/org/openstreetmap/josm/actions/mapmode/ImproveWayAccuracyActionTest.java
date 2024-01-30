// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.ImproveWayAccuracyAction.State;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.WindowlessMapViewStateMocker;
import org.openstreetmap.josm.testutils.mockers.WindowlessNavigatableComponentMocker;

import mockit.Mock;

/**
 * Unit tests for class {@link ImproveWayAccuracyAction}.
 */
@Projection
class ImproveWayAccuracyActionTest {
    @RegisterExtension
    Main.MainExtension mainExtension = new Main.MainExtension().setMapViewMocker(SizedWindowlessMapViewStateMocker::new)
            .setNavigableComponentMocker(SizedWindowlessNavigatableComponentMocker::new);

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static class SizedWindowlessMapViewStateMocker extends WindowlessMapViewStateMocker {
        @Mock
        public int getWidth() {
            return WIDTH;
        }

        @Mock
        public int getHeight() {
            return HEIGHT;
        }
    }

    private static class SizedWindowlessNavigatableComponentMocker extends WindowlessNavigatableComponentMocker {
        @Mock
        public int getWidth() {
            return WIDTH;
        }

        @Mock
        public int getHeight() {
            return HEIGHT;
        }
    }

    private static MouseEvent generateEvent(MapView mapView, ILatLon location) {
        final Point p = mapView.getPoint(location);
        return new MouseEvent(mapView, 0, 0, 0, p.x, p.y, p.x, p.y, 1, false, MouseEvent.BUTTON1);
    }

    /**
     * Unit test of {@link ImproveWayAccuracyAction#enterMode} and {@link ImproveWayAccuracyAction#exitMode}.
     */
    @Test
    void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            ImproveWayAccuracyAction mapMode = new ImproveWayAccuracyAction();
            MapFrame map = MainApplication.getMap();
            MapMode oldMapMode = map.mapMode;
            assertTrue(map.selectMapMode(mapMode));
            assertEquals(mapMode, map.mapMode);
            assertTrue(map.selectMapMode(oldMapMode));
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link State} enum.
     */
    @Test
    void testEnumState() {
        assertDoesNotThrow(() -> TestUtils.superficialEnumCodeCoverage(State.class));
    }

    @Test
    void testNonRegression23444() {
        final DataSet dataSet = new DataSet();
        final OsmDataLayer layer = new OsmDataLayer(dataSet, "ImproveWayAccuracyActionT", null);
        MainApplication.getLayerManager().addLayer(layer);
        final ImproveWayAccuracyAction mapMode = new ImproveWayAccuracyAction();
        final MapFrame map = MainApplication.getMap();
        assertTrue(map.selectMapMode(mapMode));
        assertEquals(mapMode, map.mapMode);
        final Way testWay = TestUtils.newWay("", new Node(1, 1), new Node(2, 1),
                new Node(3), new Node(4, 1), new Node(5, 1));
        testWay.firstNode().setCoor(new LatLon(0, 0));
        testWay.lastNode().setCoor(new LatLon(0.001, 0.001));
        testWay.getNode(1).setCoor(new LatLon(0.0001, 0.0001));
        testWay.getNode(3).setCoor(new LatLon(0.0009, 0.0009));
        dataSet.addPrimitiveRecursive(testWay);
        assertFalse(testWay.getNode(2).isLatLonKnown(), "The second node should not have valid coordinates");
        final Graphics2D g2d = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB).createGraphics();
        g2d.setClip(0, 0, WIDTH, HEIGHT);
        try {
            // If this fails, something else is wrong
            assertDoesNotThrow(() -> map.mapView.paint(g2d), "The mapview should be able to handle a null coordinate node");
            // Ensure that the test way is selected (and use the methods from the action to do so)
            GuiHelper.runInEDTAndWaitWithException(() -> {
                map.mapView.zoomTo(new Bounds(0, 0, 0.001, 0.001));
                // Get the target way selected (note: not selected in dataset -- use mapMode.mapReleased for that)
                mapMode.mouseMoved(generateEvent(map.mapView, testWay.getNode(1)));
            });
            // mouseMoved shouldn't cause the way to get selected
            assertFalse(dataSet.getAllSelected().contains(testWay));

            // Now check painting (where the problem should occur; the mapMode.paint call should be called as part of the map.mapView.paint call)
            assertDoesNotThrow(() -> map.mapView.paint(g2d));
            assertDoesNotThrow(() -> mapMode.paint(g2d, map.mapView, new Bounds(0, 0, 0.001, 0.001)));

            // Finally, check painting during selection
            GuiHelper.runInEDTAndWaitWithException(() -> {
                // Set the way as selected
                mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0001, 0.0001)));
                // Set the mouse location (unset in mouseReleased call)
                mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0001)));
            });
            // The way shouldn't be selected, since it isn't usable for the improve way tool
            assertFalse(dataSet.getAllSelected().contains(testWay));
            // Now check painting again (just in case)
            assertDoesNotThrow(() -> map.paint(g2d));
            assertDoesNotThrow(() -> mapMode.paint(g2d, map.mapView, new Bounds(0, 0, 0.001, 0.001)));
        } finally {
            g2d.dispose();
        }
    }
}
