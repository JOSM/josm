// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class UnJoinNodeWayAction.
 */
public final class UnJoinNodeWayActionTest {

    /**
     * Prepare the class for the test. The notification system must be disabled.
     */
    public class UnJoinNodeWayActionTestClass extends UnJoinNodeWayAction {

        /**
         * Disable notification.
         */
        @Override
        public void notify(String msg, int messageType) {
            return;
        };
    }

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test case: Ignore irrelevant nodes
     * We create a three node way, then try to remove central node while another
     * node outside is selected.
     * see #10396
     */
    @Test
    public void test10396() {
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
            Main.main.addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.map.mapView.removeLayer(layer);
        }

        // Ensures node n2 remove from w
        assertTrue("Node n2 wasn't removed from way w.",
                   !w.containsNode(n2));
    }
}
