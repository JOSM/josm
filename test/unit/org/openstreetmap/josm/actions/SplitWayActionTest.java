// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests for class {@link SplitWayAction}.
 */
public final class SplitWayActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();
    private final DataSet dataSet = new DataSet();

    private Node addNode(int east, int north) {
        final Node node = new Node(new EastNorth(east, north));
        dataSet.addPrimitive(node);
        return node;
    }

    /**
     * Test case: When node is share by multiple ways, split selected way.
     * see #11184
     */
    @Test
    public void testTicket11184() {
        Node n1 = addNode(0, 0);
        Node n2 = addNode(-1, 1);
        Node n3 = addNode(1, 1);
        Node n4 = addNode(-1, -1);
        Node n5 = addNode(1, -1);
        Node n6 = addNode(-1, 0);
        Node n7 = addNode(1, 0);

        Way w1 = new Way();
        Node[] w1NodesArray = new Node[] {n6, n1, n7};
        w1.setNodes(Arrays.asList(w1NodesArray));
        Way w2 = new Way();
        w2.setNodes(Arrays.asList(new Node[] {n1, n2, n3, n1, n4, n5, n1}));
        dataSet.addPrimitive(w1);
        dataSet.addPrimitive(w2);

        dataSet.addSelected(n1);
        dataSet.addSelected(w2);

        SplitWayAction.runOn(dataSet);

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

    /**
     * Test case: when a way is split with a turn restriction relation,
     * the relation should not be broken.
     * see #17810
     */
    @Test
    public void testTicket17810() {
        DataSet dataSet = new DataSet();
        Way from = TestUtils.newWay("highway=residential", new Node(new LatLon(0.0, 0.0)),
                new Node(new LatLon(0.00033, 0.00033)), new Node(new LatLon(0.00066, 0.00066)),
                new Node(new LatLon(0.001, 0.001)));
        from.getNodes().forEach(dataSet::addPrimitive);
        dataSet.addPrimitive(from);
        Node via = from.lastNode();
        Way to = TestUtils.newWay("highway=residential", new Node(new LatLon(0.002, 0.001)), via);
        to.getNodes().forEach(node -> {
            if (!dataSet.containsNode(node)) {
                dataSet.addPrimitive(node);
            }
        });
        dataSet.addPrimitive(to);
        Relation restriction = TestUtils.newRelation("type=restriction restriction=no_left_turn",
                new RelationMember("from", from), new RelationMember("to", to),
                new RelationMember("via", via));
        dataSet.addPrimitive(restriction);
        dataSet.clearSelection();
        dataSet.addSelected(from.getNode(2), from);
        SplitWayAction.runOn(dataSet);
        for (RelationMember member : restriction.getMembers()) {
            if ("from".equals(member.getRole())) {
                Assert.assertTrue(member.getWay().containsNode(via));
            }
        }
    }

    /**
     * Test case: smart way selection
     * see #18477
     */
    @Test
    public void testTicket18477() {
        final Node n10 = addNode(1, 0);
        final Node n21 = addNode(2, 1);
        final Way highway = TestUtils.newWay("highway=residential",
                addNode(0, 0), n10, n21, addNode(3, 1));
        final Way bridge = TestUtils.newWay("man_made=bridge",
                n10, addNode(2, 0), n21, addNode(1, 1), n10);
        dataSet.addPrimitive(highway);
        dataSet.addPrimitive(bridge);
        dataSet.setSelected(n10, n21);
        SplitWayAction.runOn(dataSet);
        assertSame(String.format("Found %d ways after split action instead of 4.", dataSet.getWays().size()),
                dataSet.getWays().size(), 4);
    }
}
