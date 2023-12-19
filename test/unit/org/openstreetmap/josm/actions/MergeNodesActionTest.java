// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link MergeNodesAction}.
 */
@Projection
class MergeNodesActionTest {
    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - empty list
     */
    @Test
    void testSelectTargetLocationNodeEmpty() {
        final List<Node> noNodes = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> MergeNodesAction.selectTargetLocationNode(noNodes));
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode} - invalid mode
     */
    @Test
    void testSelectTargetLocationNodeInvalidMode() {
        Config.getPref().putInt("merge-nodes.mode", -1);
        final List<Node> nodes = Arrays.asList(new Node(0), new Node(1));
        assertThrows(IllegalStateException.class, () -> MergeNodesAction.selectTargetLocationNode(nodes));
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetLocationNode}
     */
    @Test
    void testSelectTargetLocationNode() {
        Config.getPref().putInt("merge-nodes.mode", 0);
        assertEquals(1, MergeNodesAction.selectTargetLocationNode(Arrays.asList(new Node(0), new Node(1))).getId());

        Config.getPref().putInt("merge-nodes.mode", 1);
        assertEquals(LatLon.ZERO, MergeNodesAction.selectTargetLocationNode(
                Arrays.asList(new Node(LatLon.NORTH_POLE), new Node(LatLon.SOUTH_POLE))).getCoor());

        Config.getPref().putInt("merge-nodes.mode", 2);
        assertEquals(LatLon.NORTH_POLE, MergeNodesAction.selectTargetLocationNode(
                Collections.singletonList(new Node(LatLon.NORTH_POLE))).getCoor());
    }

    /**
     * Unit test of {@link MergeNodesAction#selectTargetNode}
     */
    @Test
    void testSelectTargetNode() {
        assertNull(MergeNodesAction.selectTargetNode(Collections.emptyList()));
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        ds.addPrimitive(n1);
        assertEquals(1, MergeNodesAction.selectTargetNode(Collections.singletonList(n1)).getId());
    }
}
