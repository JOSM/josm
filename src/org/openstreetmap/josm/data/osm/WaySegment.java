// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.awt.geom.Line2D;
import java.util.Objects;

/**
 * A segment consisting of 2 consecutive nodes out of a way.
 */
public final class WaySegment implements Comparable<WaySegment> {

    /**
     * The way.
     */
    public Way way;

    /**
     * The index of one of the 2 nodes in the way.  The other node has the
     * index <code>lowerIndex + 1</code>.
     */
    public int lowerIndex;

    /**
     * Constructs a new {@code WaySegment}.
     * @param w The way
     * @param i The node lower index
     * @throws IllegalArgumentException in case of invalid index
     */
    public WaySegment(Way w, int i) {
        way = w;
        lowerIndex = i;
        if (i < 0 || i >= w.getNodesCount() - 1) {
            throw new IllegalArgumentException(toString());
        }
    }

    /**
     * Returns the first node of the way segment.
     * @return the first node
     */
    public Node getFirstNode() {
        return way.getNode(lowerIndex);
    }

    /**
     * Returns the second (last) node of the way segment.
     * @return the second node
     */
    public Node getSecondNode() {
        return way.getNode(lowerIndex + 1);
    }

    /**
     * Determines and returns the way segment for the given way and node pair.
     * @param way way
     * @param first first node
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
    public Way toWay() {
        Way w = new Way();
        w.addNode(getFirstNode());
        w.addNode(getSecondNode());
        return w;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaySegment that = (WaySegment) o;
        return lowerIndex == that.lowerIndex &&
                Objects.equals(way, that.way);
    }

    @Override
    public int hashCode() {
        return Objects.hash(way, lowerIndex);
    }

    @Override
    public int compareTo(WaySegment o) {
        return o == null ? -1 : (equals(o) ? 0 : toWay().compareTo(o.toWay()));
    }

    /**
     * Checks whether this segment crosses other segment
     *
     * @param s2 The other segment
     * @return true if both segments crosses
     */
    public boolean intersects(WaySegment s2) {
        if (getFirstNode().equals(s2.getFirstNode()) || getSecondNode().equals(s2.getSecondNode()) ||
                getFirstNode().equals(s2.getSecondNode()) || getSecondNode().equals(s2.getFirstNode()))
            return false;

        return Line2D.linesIntersect(
                getFirstNode().getEastNorth().east(), getFirstNode().getEastNorth().north(),
                getSecondNode().getEastNorth().east(), getSecondNode().getEastNorth().north(),
                s2.getFirstNode().getEastNorth().east(), s2.getFirstNode().getEastNorth().north(),
                s2.getSecondNode().getEastNorth().east(), s2.getSecondNode().getEastNorth().north());
    }

    /**
     * Checks whether this segment and another way segment share the same points
     * @param s2 The other segment
     * @return true if other way segment is the same or reverse
     */
    public boolean isSimilar(WaySegment s2) {
        return (getFirstNode().equals(s2.getFirstNode()) && getSecondNode().equals(s2.getSecondNode()))
            || (getFirstNode().equals(s2.getSecondNode()) && getSecondNode().equals(s2.getFirstNode()));
    }

    @Override
    public String toString() {
        return "WaySegment [way=" + way.getUniqueId() + ", lowerIndex=" + lowerIndex + ']';
    }
}
