// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * A segment consisting of 2 consecutive nodes out of a way.
 */
public final class WaySegment extends IWaySegment<Node, Way> {

    /**
     * Constructs a new {@code IWaySegment}.
     *
     * @param way The way
     * @param i   The node lower index
     * @throws IllegalArgumentException in case of invalid index
     */
    public WaySegment(Way way, int i) {
        super(way, i);
    }

    /**
     * Determines and returns the way segment for the given way and node pair. You should prefer
     * {@link IWaySegment#forNodePair(IWay, INode, INode)} whenever possible.
     *
     * @param way    way
     * @param first  first node
     * @param second second node
     * @return way segment
     * @throws IllegalArgumentException if the node pair is not part of way
     */
    public static WaySegment forNodePair(Way way, Node first, Node second) {
        int endIndex = way.getNodesCount() - 1;
        while (endIndex > 0) {
            final int indexOfFirst = way.getNodes().subList(0, endIndex).lastIndexOf(first);
            if (second.equals(way.getNode(indexOfFirst + 1))) {
                return new WaySegment(way, indexOfFirst);
            }
            endIndex--;
        }
        throw new IllegalArgumentException("Node pair is not part of way!");
    }

    @Override
    public Node getFirstNode() {
        // This is kept for binary compatibility
        return super.getFirstNode();
    }

    @Override
    public Node getSecondNode() {
        // This is kept for binary compatibility
        return super.getSecondNode();
    }


    /**
     * Returns this way segment as complete way.
     * @return the way segment as {@code Way}
     */
    @Override
    public Way toWay() {
        Way w = new Way();
        w.addNode(getFirstNode());
        w.addNode(getSecondNode());
        return w;
    }

    @Override
    public boolean equals(Object o) {
        // This is kept for binary compatibility
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        // This is kept for binary compatibility
        return super.hashCode();
    }

    /**
     * Checks whether this segment crosses other segment
     *
     * @param s2 The other segment
     * @return true if both segments crosses
     */
    public boolean intersects(WaySegment s2) {
        // This is kept for binary compatibility
        return super.intersects(s2);
    }

    /**
     * Checks whether this segment and another way segment share the same points
     * @param s2 The other segment
     * @return true if other way segment is the same or reverse
     */
    public boolean isSimilar(WaySegment s2) {
        // This is kept for binary compatibility
        return super.isSimilar(s2);
    }

    @Override
    public String toString() {
        return "WaySegment [way=" + getWay().getUniqueId() + ", lowerIndex=" + getLowerIndex() + ']';
    }
}
