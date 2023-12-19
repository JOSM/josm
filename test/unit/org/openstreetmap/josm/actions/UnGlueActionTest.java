// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link UnGlueAction}.
 */
@BasicPreferences
@Main
@Projection
final class UnGlueActionTest {

    /** Class under test. */
    private static UnGlueAction action;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        if (action == null) {
            action = MainApplication.getMenu().unglueNodes;
            action.setEnabled(true);
        }
    }

    /**
     * Test without any selection.
     */
    @Test
    void testSelectionEmpty() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertTrue(ds.getSelected().isEmpty());
            assertTrue(ds.allPrimitives().isEmpty());
            action.actionPerformed(null);
            assertTrue(ds.allPrimitives().isEmpty());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that doesn't belong to a way.
     */
    @Test
    void testSingleNodeNotInWay() {
        DataSet ds = new DataSet();
        Node n = new Node(LatLon.ZERO);
        ds.addPrimitive(n);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        ds.setSelected(n);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(1, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(1, ds.allPrimitives().size());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that belongs to a single way.
     */
    @Test
    void testSingleNodeInSingleWay() {
        DataSet ds = new DataSet();
        Node n1 = new Node(LatLon.ZERO);
        ds.addPrimitive(n1);
        Node n2 = new Node(new LatLon(1.0, 1.0));
        ds.addPrimitive(n2);
        Way w = new Way();
        w.addNode(n1);
        w.addNode(n2);
        ds.addPrimitive(w);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        ds.setSelected(n1);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(3, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(3, ds.allPrimitives().size());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that belongs to two ways.
     */
    @Test
    void testSingleNodeInTwoWays() {
        DataSet ds = new DataSet();
        Node n1 = new Node(LatLon.ZERO);
        ds.addPrimitive(n1);
        Node n2 = new Node(new LatLon(1.0, 1.0));
        ds.addPrimitive(n2);
        Way w1 = new Way();
        w1.addNode(n1);
        w1.addNode(n2);
        ds.addPrimitive(w1);
        Node n3 = new Node(new LatLon(-1.0, -1.0));
        ds.addPrimitive(n3);
        Way w2 = new Way();
        w2.addNode(n1);
        w2.addNode(n3);
        ds.addPrimitive(w2);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        ds.setSelected(n1);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(5, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(6, ds.allPrimitives().size());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
