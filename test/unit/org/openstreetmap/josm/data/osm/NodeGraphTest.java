// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the {@code NodeGraph} class.
 */
class NodeGraphTest {
    /**
     * Unit test of {@link NodeGraph#buildNodePairs} and {@link NodeGraph#eliminateDuplicateNodePairs}
     */
    @Test
    void testNodePairs() {
        assertTrue(NodeGraph.buildNodePairs(Collections.emptyList(), true).isEmpty());
        assertTrue(NodeGraph.buildNodePairs(Collections.emptyList(), false).isEmpty());

        Way w1 = new Way(1);
        Way w2 = new Way(2);

        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);

        Node n4 = new Node(4);
        Node n5 = new Node(5);
        Node n6 = new Node(6);

        w1.setNodes(Arrays.asList(n1, n2, n3));
        w2.setNodes(Arrays.asList(n4, n5, n6, n4));

        w1.setIncomplete(false);
        w2.setIncomplete(false);

        List<Way> ways = Arrays.asList(w1, w2);

        List<NodePair> l1 = NodeGraph.buildNodePairs(ways, true);
        List<NodePair> l2 = NodeGraph.buildNodePairs(ways, false);

        assertEquals(Arrays.asList(
                new NodePair(n1, n2),
                new NodePair(n2, n3),
                new NodePair(n4, n5),
                new NodePair(n5, n6),
                new NodePair(n6, n4)
                ), l1);

        assertEquals(l1, NodeGraph.eliminateDuplicateNodePairs(l1));

        assertEquals(Arrays.asList(
                new NodePair(n1, n2), new NodePair(n2, n1),
                new NodePair(n2, n3), new NodePair(n3, n2),
                new NodePair(n4, n5), new NodePair(n5, n4),
                new NodePair(n5, n6), new NodePair(n6, n5),
                new NodePair(n6, n4), new NodePair(n4, n6)
                ), l2);

        assertEquals(l1, NodeGraph.eliminateDuplicateNodePairs(l2));
    }
}
