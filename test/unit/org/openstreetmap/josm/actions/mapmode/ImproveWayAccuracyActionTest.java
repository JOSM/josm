// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.function.TriConsumer;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.ImproveWayAccuracyAction.State;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link ImproveWayAccuracyAction}.
 */
@Main
@Projection
class ImproveWayAccuracyActionTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private static class AlwaysDeleteCallback implements DeleteCommand.DeletionCallback {
        @Override
        public boolean checkAndConfirmOutlyingDelete(Collection<? extends OsmPrimitive> primitives, Collection<? extends OsmPrimitive> ignore) {
            return true;
        }

        @Override
        public boolean confirmRelationDeletion(Collection<Relation> relations) {
            return true;
        }

        @Override
        public boolean confirmDeletionFromRelation(Collection<RelationToChildReference> references) {
            return true;
        }
    }

    private static void setupMapView(DataSet ds) {
        // setup a reasonable size for the edit window
        MainApplication.getMap().mapView.setBounds(new Rectangle(WIDTH, HEIGHT));
        if (ds.getDataSourceBoundingBox() != null) {
            MainApplication.getMap().mapView.zoomTo(ds.getDataSourceBoundingBox());
        } else {
            BoundingXYVisitor v = new BoundingXYVisitor();
            for (Layer l : MainApplication.getLayerManager().getLayers()) {
                l.visitBoundingBox(v);
            }
            MainApplication.getMap().mapView.zoomTo(v);
        }
    }

    /**
     * Generate a mouse event
     * @param mapView The current map view
     * @param location The location to generate the event for
     * @param modifiers The modifiers for {@link MouseEvent} (see {@link InputEvent#getModifiersEx()})
     * @return The generated event
     */
    private static MouseEvent generateEvent(MapView mapView, ILatLon location, int modifiers) {
        final Point p = mapView.getPoint(location);
        return new MouseEvent(mapView, 0, 0, modifiers, p.x, p.y, p.x, p.y, 1, false, MouseEvent.BUTTON1);
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
    void testNonRegression23444Selection() {
        final DataSet dataSet = new DataSet();
        final OsmDataLayer layer = new OsmDataLayer(dataSet, "ImproveWayAccuracyActionTest#testNonRegression23444Selection", null);
        MainApplication.getLayerManager().addLayer(layer);
        final ImproveWayAccuracyAction mapMode = new ImproveWayAccuracyAction();
        final MapFrame map = MainApplication.getMap();
        final Way testWay = TestUtils.newWay("", new Node(1, 1), new Node(2, 1),
                new Node(3), new Node(4, 1), new Node(5, 1));
        testWay.firstNode().setCoor(new LatLon(0, 0));
        testWay.lastNode().setCoor(new LatLon(0.001, 0.001));
        testWay.getNode(1).setCoor(new LatLon(0.0001, 0.0001));
        testWay.getNode(3).setCoor(new LatLon(0.0009, 0.0009));
        dataSet.addPrimitiveRecursive(testWay);
        assertFalse(testWay.getNode(2).isLatLonKnown(), "The second node should not have valid coordinates");
        dataSet.setSelected(testWay.firstNode());
        assertTrue(map.selectMapMode(mapMode));
        assertEquals(mapMode, map.mapMode);
        // This is where the exception occurs; we shouldn't be setting the incomplete way as the target when we enter the mode.
        setupMapView(dataSet);
        assertDoesNotThrow(() -> GuiHelper.runInEDTAndWaitWithException(() -> {
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0001), 0));
        }));
    }

    @Test
    void testNonRegression23444() {
        testSimplifyWayAction((mapMode, map, testWay) -> {
            testWay.getNode(2).setCoor(null);
            assertFalse(testWay.getNode(2).isLatLonKnown(), "The second node should not have valid coordinates");
            mapMode.startSelecting();
            mapMode.mouseMoved(generateEvent(map.mapView, testWay.getNode(1), 0));
        });
    }

    @Test
    void testAdd() {
        AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Add a node at 0.0001, 0.0005 (not on the direct line)
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), InputEvent.CTRL_DOWN_MASK));
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), InputEvent.CTRL_DOWN_MASK));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        // There should be a new node between nodes 1 and 2 (old node 2 is now node 3)
        assertAll(() -> assertEquals(6, testWay.getNodesCount()),
                () -> assertFalse(testWay.getNode(1).isNew()),
                () -> assertTrue(testWay.getNode(2).isNew()),
                () -> assertFalse(testWay.getNode(3).isNew()));
        // These aren't expected to be 0.0001 and 0.0005 exactly, due zoom and conversions between point and latlon.
        assertAll(() -> assertEquals(0.0001, testWay.getNode(2).lat(), 1e-5),
                () -> assertEquals(0.0005, testWay.getNode(2).lon(), 1e-5));
    }

    @Test
    void testAddLock() {
        final AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Add a node at 0.0009, 0.0005 (not on the direct line) that is between nodes 1 and 2 but not 2 and 3.
            // First get the waysegment selected
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), InputEvent.CTRL_DOWN_MASK));
            // Then move to another location with ctrl+shift
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0009, 0.0005), InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            // Finally, release the mouse with ctrl+shift
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0009, 0.0005),
                    InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        // There should be a new node between nodes 1 and 2 (old node 2 is now node 3)
        assertAll(() -> assertEquals(6, testWay.getNodesCount()),
                () -> assertFalse(testWay.getNode(1).isNew()),
                () -> assertTrue(testWay.getNode(2).isNew()),
                () -> assertFalse(testWay.getNode(3).isNew()));
        // These aren't expected to be 0.0009 and 0.0005 exactly, due zoom and conversions between point and latlon.
        assertAll(() -> assertEquals(0.0009, testWay.getNode(2).lat(), 1e-5),
                () -> assertEquals(0.0005, testWay.getNode(2).lon(), 1e-5));
    }

    @Test
    void testMove() {
        final AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Move node to 0.0001, 0.0005 (not on the direct line)
            // First get the waysegment selected
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), 0));
            // Finally, release the mouse
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), 0));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        assertEquals(5, testWay.getNodesCount());
        // These aren't expected to be 0.0001 and 0.0005 exactly, due zoom and conversions between point and latlon.
        assertAll(() -> assertEquals(0.0001, testWay.getNode(2).lat(), 1e-5),
                () -> assertEquals(0.0005, testWay.getNode(2).lon(), 1e-5));
    }

    @Test
    void testMoveLock() {
        final AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Move node to 0.0001, 0.0005 (not on the direct line)
            // First get the waysegment selected
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), 0));
            // Then move to another location
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0009, 0.0005), InputEvent.SHIFT_DOWN_MASK));
            // Finally, release the mouse
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0009, 0.0005), InputEvent.SHIFT_DOWN_MASK));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        assertEquals(5, testWay.getNodesCount());
        // These aren't expected to be 0.0009 and 0.0005 exactly, due zoom and conversions between point and latlon.
        assertAll(() -> assertEquals(0.0009, testWay.getNode(2).lat(), 1e-5),
                () -> assertEquals(0.0005, testWay.getNode(2).lon(), 1e-5));
    }

    @Test
    void testDelete() {
        DeleteCommand.setDeletionCallback(new AlwaysDeleteCallback());
        final AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Move node to 0.0001, 0.0005 (not on the direct line)
            // First get the waysegment selected
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), InputEvent.ALT_DOWN_MASK));
            // Finally, release the mouse
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), InputEvent.ALT_DOWN_MASK));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        assertEquals(4, testWay.getNodesCount());
        assertAll(testWay.getNodes().stream().map(n -> () -> assertNotEquals(3, n.getUniqueId())));
    }

    @Test
    void testDeleteLock() {
        DeleteCommand.setDeletionCallback(new AlwaysDeleteCallback());
        final AtomicReference<Way> referenceWay = new AtomicReference<>();
        testSimplifyWayAction((mapMode, map, testWay) -> {
            // Move node to 0.0001, 0.0005 (not on the direct line)
            // First get the waysegment selected
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0005), 0));
            // Then move to another location
            mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0009, 0.0005), InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
            // Finally, release the mouse
            mapMode.mouseReleased(generateEvent(map.mapView, new LatLon(0.0009, 0.0005), InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
            referenceWay.set(testWay);
        });
        final Way testWay = referenceWay.get();
        assertEquals(4, testWay.getNodesCount());
        assertAll(testWay.getNodes().stream().map(n -> () -> assertNotEquals(3, n.getUniqueId())));
    }

    private void testSimplifyWayAction(TriConsumer<ImproveWayAccuracyAction, MapFrame, Way> runnable) {
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
        testWay.getNode(2).setCoor(new LatLon(0.0005, 0.0005));
        testWay.getNode(3).setCoor(new LatLon(0.0009, 0.0009));
        dataSet.addPrimitiveRecursive(testWay);
        setupMapView(dataSet);
        final Graphics2D g2d = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB).createGraphics();
        g2d.setClip(0, 0, WIDTH, HEIGHT);
        try {
            // If this fails, something else is wrong
            assertDoesNotThrow(() -> map.mapView.paint(g2d), "The mapview should be able to handle a null coordinate node");
            // Ensure that the test way is selected (and use the methods from the action to do so)
            assertDoesNotThrow(() -> GuiHelper.runInEDTAndWaitWithException(() -> {
                // Set the way as selected
                mapMode.mouseMoved(generateEvent(map.mapView, testWay.getNode(1), 0));
                mapMode.mouseReleased(generateEvent(map.mapView, testWay.getNode(1), 0));
                // And then run the test case
                runnable.accept(mapMode, map, testWay);
            }));

            // Now check painting (where the problem should occur; the mapMode.paint call should be called as part of the map.mapView.paint call)
            assertDoesNotThrow(() -> map.mapView.paint(g2d));
            assertDoesNotThrow(() -> mapMode.paint(g2d, map.mapView, new Bounds(0, 0, 0.001, 0.001)));

            // Then perform the action(s)
            GuiHelper.runInEDTAndWaitWithException(() -> {
                // Set the mouse location (unset in mouseReleased call)
                // This is required by testNonRegression23444, and it doesn't hurt during the other tests
                mapMode.mouseMoved(generateEvent(map.mapView, new LatLon(0.0001, 0.0001), 0));
            });
            // Now check painting again (just in case)
            assertDoesNotThrow(() -> map.paint(g2d));
            assertDoesNotThrow(() -> mapMode.paint(g2d, map.mapView, new Bounds(0, 0, 0.001, 0.001)));
        } finally {
            g2d.dispose();
        }
    }
}
