// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link SimplifyWayAction}.
 */
public final class SimplifyWayActionTest {

    /** Class under test. */
    private static SimplifyWayAction action;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
        action = Main.main.menu.simplifyWay;
        action.setEnabled(true);
    }

    private static Way createWaySelected(DataSet ds, double latStart) {
        Node n1 = new Node(new LatLon(latStart, 1.0));
        ds.addPrimitive(n1);
        Node n2 = new Node(new LatLon(latStart+1.0, 1.0));
        ds.addPrimitive(n2);
        Way w = new Way();
        w.addNode(n1);
        w.addNode(n2);
        ds.addPrimitive(w);
        ds.addSelected(w);
        return w;
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
            action.actionPerformed(null);
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with a single way.
     */
    @Test
    public void testSingleWay() {
        DataSet ds = new DataSet();
        createWaySelected(ds, 0.0);
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            action.actionPerformed(null);
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test with more than 10 ways.
     */
    @Test
    public void testMoreThanTenWays() {
        DataSet ds = new DataSet();
        for (int i = 0; i < 11; i++) {
            createWaySelected(ds, i);
        }
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            assertEquals(11, ds.getSelected().size());
            action.actionPerformed(null);
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
