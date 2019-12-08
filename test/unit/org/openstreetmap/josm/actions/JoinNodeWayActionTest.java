// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Geometry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link JoinNodeWayAction}.
 */
public final class JoinNodeWayActionTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().main().preferences().projection();

    private void setupMapView(DataSet ds) {
        // setup a reasonable screen size
        MainApplication.getMap().mapView.setBounds(new Rectangle(1920, 1080));
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
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket18189() throws Exception {
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
            assertTrue("Node n5 wasn't added to way w1.", w1.containsNode(n5));
            assertTrue("Node n5 wasn't added to way w2.", w2.containsNode(n5));
            assertTrue("Node was moved to an unexpected position", n5.getEastNorth().equalsEpsilon(expected, 1e-7));
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/11508">Bug #11508</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket11508() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(11508, "11508_example.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertTrue(nodesToMove.size() == 1);
            Node toMove = nodesToMove.iterator().next();
            Node expected = new Node(new LatLon(47.56331849690742, 8.800789259499311));
            ds.setSelected(toMove);
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            action.actionPerformed(null);

            assertTrue("Node was moved to an unexpected position", toMove.getEastNorth().equalsEpsilon(expected.getEastNorth(), 1e-7));
            assertTrue("Node was not added to expected number of ways", toMove.getParentWays().size() == 2);
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Check that nothing is changed if ways are too far.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket18189Crossing() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18189, "moveontocrossing.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertTrue(nodesToMove.size() == 1);
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
    public void testTicket18189ThreeWays() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(18189, "data.osm"), null);
        Layer layer = new OsmDataLayer(ds, OsmDataLayer.createNewName(), null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            setupMapView(ds);
            JoinNodeWayAction action = JoinNodeWayAction.createMoveNodeOntoWayAction();
            action.setEnabled(true);
            List<Node> nodesToMove = ds.getNodes().stream().filter(n -> n.hasTag("name", "select me and press N"))
                    .collect(Collectors.toList());
            assertTrue(nodesToMove.size() == 1);
            Node toMove = nodesToMove.iterator().next();
            Node expected = new Node(new LatLon(-21.088998104148224, -50.38629102179512));
            ds.setSelected(toMove);
            action.actionPerformed(null);
            assertTrue("Node was moved to an unexpected position", toMove.getEastNorth().equalsEpsilon(expected.getEastNorth(), 1e-7));
            assertTrue("Node was not added to expected number of ways", toMove.getParentWays().size() == 4);

        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

}
