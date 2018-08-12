// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MergeNodesAction}.
 */
public class MergeNodesActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - empty list
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSelectTargetLocationNodeEmpty() {
        MergeNodesAction.selectTargetLocationNode(Collections.emptyList());
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - invalid mode
     */
    @Test(expected = IllegalStateException.class)
    public void testSelectTargetLocationNodeInvalidMode() {
        Config.getPref().putInt("merge-nodes.mode", -1);
        MergeNodesAction.selectTargetLocationNode(Arrays.asList(new Node(0), new Node(1)));
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode}
     */
    @Test
    public void testSelectTargetLocationNode() {
        Config.getPref().putInt("merge-nodes.mode", 0);
        assertEquals(1, MergeNodesAction.selectTargetLocationNode(Arrays.asList(new Node(0), new Node(1))).getId());

        Config.getPref().putInt("merge-nodes.mode", 1);
        assertEquals(LatLon.ZERO, MergeNodesAction.selectTargetLocationNode(
                Arrays.asList(new Node(LatLon.NORTH_POLE), new Node(LatLon.SOUTH_POLE))).getCoor());

        Config.getPref().putInt("merge-nodes.mode", 2);
        assertEquals(LatLon.NORTH_POLE, MergeNodesAction.selectTargetLocationNode(
                Arrays.asList(new Node(LatLon.NORTH_POLE))).getCoor());
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetNode}
     */
    @Test
    public void testSelectTargetNode() {
        assertNull(MergeNodesAction.selectTargetNode(Collections.emptyList()));
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        ds.addPrimitive(n1);
        assertEquals(1, MergeNodesAction.selectTargetNode(Arrays.asList(n1)).getId());
    }
}
