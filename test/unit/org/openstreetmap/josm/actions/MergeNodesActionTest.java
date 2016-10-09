// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
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
    public JOSMTestRules test = new JOSMTestRules().platform().commands();

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
        Main.pref.putInteger("merge-nodes.mode", -1);
        MergeNodesAction.selectTargetLocationNode(Arrays.asList(new Node(0), new Node(1)));
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - mode 0
     */
    @Test
    public void testSelectTargetLocationNodeMode0() {
        Main.pref.putInteger("merge-nodes.mode", 0);
        assertEquals(1, MergeNodesAction.selectTargetLocationNode(Arrays.asList(new Node(0), new Node(1))).getId());
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - mode 1
     */
    @Test
    public void testSelectTargetLocationNodeMode1() {
        Main.pref.putInteger("merge-nodes.mode", 1);
        assertEquals(LatLon.ZERO, MergeNodesAction.selectTargetLocationNode(
                Arrays.asList(new Node(LatLon.NORTH_POLE), new Node(LatLon.SOUTH_POLE))).getCoor());
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - mode 2 with a single node
     */
    @Test
    public void testSelectTargetLocationNodeMode2SingleNode() {
        Main.pref.putInteger("merge-nodes.mode", 2);
        assertEquals(LatLon.NORTH_POLE, MergeNodesAction.selectTargetLocationNode(
                Arrays.asList(new Node(LatLon.NORTH_POLE))).getCoor());
    }
}
