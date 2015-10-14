// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertSame;
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
 * Unit tests for class {@link SplitWayAction}.
 */
public final class SplitWayActionTest {

    /** Class under test. */
    private static SplitWayAction action;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);

        // Enable "Align in line" feature.
        action = Main.main.menu.splitWay;
        action.setEnabled(true);
    }

    /**
     * Test case: When node is share by multiple ways, split selected way.
     * see #11184
     */
    @Test
    public void testTicket11184() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        Node n4 = new Node(new EastNorth(-1, -1));
        Node n5 = new Node(new EastNorth(1, -1));
        Node n6 = new Node(new EastNorth(-1, 0));
        Node n7 = new Node(new EastNorth(1, 0));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);
        dataSet.addPrimitive(n4);
        dataSet.addPrimitive(n5);
        dataSet.addPrimitive(n6);
        dataSet.addPrimitive(n7);

        Way w1 = new Way();
        Node[] w1NodesArray = new Node[] {n6, n1, n7};
        w1.setNodes(Arrays.asList(w1NodesArray));
        Way w2 = new Way();
        w2.setNodes(Arrays.asList(new Node[] {n1, n2, n3, n1, n4, n5, n1}));
        dataSet.addPrimitive(w1);
        dataSet.addPrimitive(w2);

        dataSet.addSelected(n1);
        dataSet.addSelected(w2);

        try {
            Main.main.addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.map.mapView.removeLayer(layer);
        }

        // Ensures 3 ways.
        assertSame(String.format("Found %d ways after split action instead of 3.", dataSet.getWays().size()),
                   dataSet.getWays().size(), 3);

        // Ensures way w1 is unchanged.
        assertTrue("Unselected ways disappear during split action.",
                   dataSet.getWays().contains(w1));
        assertSame("Unselected way seems to have change during split action.",
                   w1.getNodesCount(), 3);
        for (int i = 0; i < 3; i++) {
            assertSame("Node change in unselected way during split action.",
                       w1.getNode(i), w1NodesArray[i]);
        }
    }
}
