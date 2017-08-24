// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link UnJoinNodeWayAction}.
 */
public final class UnJoinNodeWayActionTest {

    /**
     * Prepare the class for the test. The notification system must be disabled.
     */
    public static class UnJoinNodeWayActionTestClass extends UnJoinNodeWayAction {

        /**
         * Disable notification.
         */
        @Override
        public void notify(String msg, int messageType) {
            return;
        }
    }

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Test case: Ignore irrelevant nodes
     * We create a three node way, then try to remove central node while another
     * node outside is selected.
     * see #10396
     */
    @Test
    public void testTicket10396() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(-1, -1));
        Node n2 = new Node(new EastNorth(0, 0));
        Node n3 = new Node(new EastNorth(1, -1));
        Node n4 = new Node(new EastNorth(0, 1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);
        dataSet.addPrimitive(n4);

        Way w = new Way();
        w.setNodes(Arrays.asList(new Node[] {n1, n2, n3}));
        dataSet.addPrimitive(w);

        dataSet.addSelected(n2);
        dataSet.addSelected(n4);
        dataSet.addSelected(w);

        UnJoinNodeWayActionTestClass action = new UnJoinNodeWayActionTestClass();
        action.setEnabled(true);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }

        // Ensures node n2 remove from w
        assertFalse("Node n2 wasn't removed from way w.", w.containsNode(n2));
    }
}
