// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SimplifyWayAction}.
 */
public final class SimplifyWayActionTest {

    /** Class under test. */
    private static SimplifyWayAction action;

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
            action = MainApplication.getMenu().simplifyWay;
            action.setEnabled(true);
        }
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
            MainApplication.getLayerManager().addLayer(layer);
            assertTrue(ds.getSelected().isEmpty());
            action.actionPerformed(null);
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
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
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(1, ds.getSelected().size());
            action.actionPerformed(null);
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
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
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(11, ds.getSelected().size());
            action.actionPerformed(null);
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Tests that also the first node may be simplified, see #13094.
     */
    @Test
    public void testSimplifyFirstNode() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(new LatLon(47.26269614984, 11.34044231149));
        final Node n2 = new Node(new LatLon(47.26274590831, 11.34053120859));
        final Node n3 = new Node(new LatLon(47.26276562382, 11.34034715039));
        final Node n4 = new Node(new LatLon(47.26264639132, 11.34035341438));
        final Way w = new Way();
        Stream.of(n1, n2, n3, n4, w).forEach(ds::addPrimitive);
        Stream.of(n1, n2, n3, n4, n1).forEach(w::addNode);
        final SequenceCommand command = action.simplifyWay(w);
        assertNotNull(command);
        assertEquals(2, command.getChildren().size());
        final Collection<DeleteCommand> deleteCommands = Utils.filteredCollection(command.getChildren(), DeleteCommand.class);
        assertEquals(1, deleteCommands.size());
        assertEquals(Collections.singleton(n1), deleteCommands.iterator().next().getParticipatingPrimitives());
    }
}
