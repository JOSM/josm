// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Objects;

import org.openstreetmap.josm.tools.Pair;

/**
 * A directed pair of nodes (a,b != b,a).
 * @since 12463 (extracted from CombineWayAction)
 */
public class NodePair {
    private final Node a;
    private final Node b;

    /**
     * Constructs a new {@code NodePair}.
     * @param a The first node
     * @param b The second node
     */
    public NodePair(Node a, Node b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Constructs a new {@code NodePair}.
     * @param pair An existing {@code Pair} of nodes
     */
    public NodePair(Pair<Node, Node> pair) {
        this(pair.a, pair.b);
    }

    /**
     * Replies the first node.
     * @return The first node
     */
    public Node getA() {
        return a;
    }

    /**
     * Replies the second node
     * @return The second node
     */
    public Node getB() {
        return b;
    }

    /**
     * Determines if this pair is successor of another one (other.b == this.a)
     * @param other other pair
     * @return {@code true} if other.b == this.a
     */
    public boolean isSuccessorOf(NodePair other) {
        return other.getB() == a;
    }

    /**
     * Determines if this pair is predecessor of another one (this.b == other.a)
     * @param other other pair
     * @return {@code true} if this.b == other.a
     */
    public boolean isPredecessorOf(NodePair other) {
        return b == other.getA();
    }

    /**
     * Returns the inversed pair.
     * @return swapped copy
     */
    public NodePair swap() {
        return new NodePair(b, a);
    }

    @Override
    public String toString() {
        return "[" +
                a.getId() +
                ',' +
                b.getId() +
                ']';
    }

    /**
     * Determines if this pair contains the given node.
     * @param n The node to look for
     * @return {@code true} if {@code n} is in the pair, {@code false} otherwise
     */
    public boolean contains(Node n) {
        return a == n || b == n;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NodePair nodePair = (NodePair) obj;
        return Objects.equals(a, nodePair.a) &&
               Objects.equals(b, nodePair.b);
    }
}
