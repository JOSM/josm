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
    public String toString() {
        return "WaySegment [way=" + getWay().getUniqueId() + ", lowerIndex=" + getLowerIndex() + ']';
    }
}
