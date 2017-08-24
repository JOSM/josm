// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link UnGlueAction}.
 */
public final class UnGlueActionTest {

    /** Class under test. */
    private static UnGlueAction action;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        if (action == null) {
            action = Main.main.menu.unglueNodes;
            action.setEnabled(true);
        }
    }

    /**
     * Test without any selection.
     */
    @Test
    public void testSelectionEmpty() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            assertTrue(ds.getSelected().isEmpty());
            assertTrue(ds.allPrimitives().isEmpty());
            action.actionPerformed(null);
            assertTrue(ds.allPrimitives().isEmpty());
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that doesn't belong to a way.
     */
    @Test
    public void testSingleNodeNotInWay() {
        DataSet ds = new DataSet();
        Node n = new Node(LatLon.ZERO);
        ds.addPrimitive(n);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        ds.setSelected(n);
        try {
            Main.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(1, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(1, ds.allPrimitives().size());
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that belongs to a single way.
     */
    @Test
    public void testSingleNodeInSingleWay() {
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
            Main.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(3, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(3, ds.allPrimitives().size());
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single node, that belongs to two ways.
     */
    @Test
    public void testSingleNodeInTwoWays() {
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
            Main.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            assertEquals(5, ds.allPrimitives().size());
            action.actionPerformed(null);
            assertEquals(6, ds.allPrimitives().size());
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
