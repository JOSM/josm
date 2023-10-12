package org.openstreetmap.josm.actions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for class {@link DistributeAction}.
 */
@Main
@Projection
class DistributeActionTest {

    private static final Random random = new Random();
    private DataSet ds = new DataSet();

    @BeforeEach
    void setUp() {
        ds = new DataSet();
    }

    @Test
    void testNoAlignment() {
        Node n = new Node(LatLon.ZERO);
        ds.addPrimitive(n);

        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());

        // select a single node
        layer.getDataSet().setSelected(n.getPrimitiveId());
        assertEquals(1, layer.getDataSet().getSelected().size());

        new DistributeAction().actionPerformed(null);
        assertEquals(n.lat(), LatLon.ZERO.lat());
        assertEquals(n.lon(), LatLon.ZERO.lon());
    }

    @Test
    void testWholeWayAlignment() {
        Way way = new Way();
        final int totalNodeCount = 11;  // should be in range [2,180]!
        final int innerNodeCount = totalNodeCount - 2;
        final int lastLon = totalNodeCount - 1;

        // add first node
        Node n = new Node(new LatLon(LatLon.ZERO));
        ds.addPrimitive(n);
        way.addNode(n);

        // add interim nodes
        for (int i = 0; i < innerNodeCount; i++) {
            n = new Node(new LatLon(0, getRandomDoubleInRange(0, lastLon)));
            ds.addPrimitive(n);
            way.addNode(n);
        }

        // add last node
        n = new Node(new LatLon(0, lastLon));
        ds.addPrimitive(n);
        way.addNode(n);
        ds.addPrimitive(way);


        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());

        // select the way
        layer.getDataSet().setSelected(way.getPrimitiveId());
        assertEquals(1, layer.getDataSet().getSelected().size());

        new DistributeAction().actionPerformed(null);

        for (int i = 0; i < totalNodeCount; i++) {
            assertEquals(
                    (double) (1 / lastLon) + i,
                    way.getNode(i).lon(),
                    1e-7
            );
        }
    }

    @Test
    void testNodesAlignment() {
        Way way = new Way();
        final int totalNodeCount = 11;  // should be in range [2,180]!
        final int innerNodeCount = totalNodeCount - 2;
        final int lastLon = totalNodeCount - 1;

        // add first node
        Node n = new Node(new LatLon(LatLon.ZERO));
        ds.addPrimitive(n);
        way.addNode(n);

        // add interim nodes
        for (int i = 0; i < innerNodeCount; i++) {
            n = new Node(new LatLon(0, getRandomDoubleInRange(0, lastLon)));
            ds.addPrimitive(n);
            way.addNode(n);
        }

        // add last node
        n = new Node(new LatLon(0, lastLon));
        ds.addPrimitive(n);
        way.addNode(n);
        ds.addPrimitive(way);


        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());

        // select all nodes on the way
        layer.getDataSet().setSelected(way.getNodes());
        assertEquals(way.getNodes().size(), layer.getDataSet().getSelected().size());

        new DistributeAction().actionPerformed(null);

        // FIXME: the assertion will most likely fail due to the current core implementation:
        //  the two *furthest nodes* are selected as alignment base, then they evenly ordered along a virtual way.
        //  Test expectation: the *end nodes* of a virtual way are distribution basis.
        for (int i = 0; i < totalNodeCount; i++) {
            assertEquals(
                    (double) (1 / lastLon) + i,
                    way.getNode(i).lon(),
                    1e-7
            );
        }
    }

    @Test
    void testSingleNodeAlignment() {
        Way way = new Way();
        final int totalNodeCount = 11;  // should be in range [3,180]!
        final int lastLon = totalNodeCount - 1;

        Node n;
        Node selectedNode = null;

        // add nodes except the last one
        for (int i = 0; i < totalNodeCount - 1; i++) {
            if (i == 1) {
                n = new Node(new LatLon(0, 0.1));
                selectedNode = n;
            } else {
                n = new Node(new LatLon(0, i));
            }
            ds.addPrimitive(n);
            way.addNode(n);
        }

        // add last node
        n = new Node(new LatLon(0, lastLon));
        ds.addPrimitive(n);
        way.addNode(n);
        ds.addPrimitive(way);


        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        assertNotNull(MainApplication.getLayerManager().getActiveLayer());

        // select a single node
        layer.getDataSet().setSelected(selectedNode.getPrimitiveId());
        assertEquals(1, layer.getDataSet().getSelected().size());

        new DistributeAction().actionPerformed(null);

        for (int i = 0; i < totalNodeCount; i++) {
            assertEquals(
                    (double) (1 / lastLon) + i,
                    way.getNode(i).lon(),
                    1e-7
            );
        }
    }

    private static double getRandomDoubleInRange(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("Invalid range. Max must be greater than min.");
        }
        return min + (max - min) * random.nextDouble();
    }
}