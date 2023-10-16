// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Unit tests for class {@link JoinNodeWayAction}.
 */
@BasicPreferences
@Main
@Projection
final class JoinNodeWayActionTest {
    private void setupMapView(DataSet ds) {
        // setup a reasonable size for the edit window
        MainApplication.getMap().mapView.setBounds(new Rectangle(1345, 939));
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
     * Test case: Move node onto two almost overlapping ways
     * see #18189 moveontoway.osm
     */
    @Test
    void testTicket18189() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Node n1 = new Node(new LatLon(59.92881498658, 30.30104052971));
            Node n2 = new Node(new LatLon(59.92881459851, 30.30104056556));
            Node n3 = new Node(new LatLon(59.92881498658, 30.3010405297));
            Node n4 = new Node(new LatLon(59.92881459851, 30.30104056556));
            Node n5 = new Node(new LatLon(59.92881483122, 30.30104056465));

            dataSet.addPrimitive(n1);
            dataSet.addPrimitive(n2);
            dataSet.addPrimitive(n3);
            dataSet.addPrimitive(n4);
            dataSet.addPrimitive(n5);

            Way w1 = new Way();
            w1.setNodes(Arrays.asList(n1, n2));
            dataSet.addPrimitive(w1);
            Way w2 = new Way();
            w2.setNodes(Arrays.asList(n3, n4));
            dataSet.addPrimitive(w2);

            dataSet.addSelected(n5);
            EastNorth expected = Geometry.closestPointToSegment(n1.getEastNorth(), n2.getEastNorth(), n5.getEastNorth());

            setupMapView(dataSet);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            action.actionPerformed(null);
            // Make sure the node was only moved once
            assertTrue(w1.containsNode(n5), "Node n5 wasn't added to way w1.");
            assertTrue(w2.containsNode(n5), "Node n5 wasn't added to way w2.");
            assertTrue(n5.getEastNorth().equalsEpsilon(expected, 1e-7), "Node was moved to an unexpected position");
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/11508">Bug #11508</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket11508() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(11508, "11508_example.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertEquals(1, nodesToMove.size());
            Node toMove = nodesToMove.iterator().next();
            Node expected = new Node(new LatLon(47.56331849690742, 8.800789259499311));
            ds.setSelected(toMove);
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            action.actionPerformed(null);

            assertTrue(toMove.getEastNorth().equalsEpsilon(expected.getEastNorth(), 1e-7), "Node was moved to an unexpected position");
            assertEquals(2, toMove.getParentWays().size(), "Node was not added to expected number of ways");
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Check that nothing is changed if ways are too far.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket18189Crossing() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18189, "moveontocrossing.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertEquals(1, nodesToMove.size());
            Node toMove = nodesToMove.iterator().next();
            ds.setSelected(toMove);
            action.actionPerformed(null);
            assertTrue(toMove.getParentWays().isEmpty());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Check that nothing is changed if ways are too far.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket18189ThreeWays() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18189, "data.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertEquals(1, nodesToMove.size());
            Node toMove = nodesToMove.iterator().next();
            Node expected = new Node(new LatLon(-21.088998104148224, -50.38629102179512));
            ds.setSelected(toMove);
            action.actionPerformed(null);
            assertTrue(toMove.getEastNorth().equalsEpsilon(expected.getEastNorth(), 1e-7), "Node was moved to an unexpected position");
            assertEquals(4, toMove.getParentWays().size(), "Node was not added to expected number of ways");
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18420">Bug #18420</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket18420() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18420, "user-sample.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name")).collect(Collectors.toList());
            assertEquals(2, nodesToMove.size());
            Node n = nodesToMove.iterator().next();
            if (!n.hasTag("name", "select me 1st"))
                Collections.reverse(nodesToMove);
            Node toMove1 = nodesToMove.get(0);
            Node toMove2 = nodesToMove.get(1);
            Node expected1 = new Node(new LatLon(49.8546658263727, 6.206059532463773));
            Node expected2 = new Node(new LatLon(49.854738602108085, 6.206213646054511));
            ds.setSelected(nodesToMove);
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            action.actionPerformed(null);
            assertTrue(toMove1.getEastNorth().equalsEpsilon(expected1.getEastNorth(), 1e-7), "Node was moved to an unexpected position");
            assertTrue(toMove2.getEastNorth().equalsEpsilon(expected2.getEastNorth(), 1e-7), "Node was moved to an unexpected position");
            assertEquals(2, toMove1.getParentWays().size(), "Node was not added to expected number of ways");
            assertEquals(2, toMove2.getParentWays().size(), "Node was not added to expected number of ways");
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18990">Bug #18990</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket18990() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18990, "18990-sample.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Node toMove = (Node) ds.getPrimitiveById(new SimplePrimitiveId(7018586511L, OsmPrimitiveType.NODE));
            assertNotNull(toMove);
            Node expected = new Node(new LatLon(43.48582074476985, -96.76897750613033));

            ds.setSelected(toMove);
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            action.actionPerformed(null);
            assertTrue(toMove.getEastNorth().equalsEpsilon(expected.getEastNorth(), 1e-7), "Node was moved to an unexpected position");
            assertEquals(1, toMove.getParentWays().size(), "Node was not added to expected way");
            assertEquals(2, toMove.getParentWays().iterator().next().getNodes().indexOf(toMove), "Node was not added to expected way segment");
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

}
