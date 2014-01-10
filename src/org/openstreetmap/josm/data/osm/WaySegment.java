// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.awt.geom.Line2D;

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
     */
    public WaySegment(Way w, int i) {
        way = w;
        lowerIndex = i;
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
    public Node getSecondNode(){
        return way.getNode(lowerIndex + 1);
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
        return o instanceof WaySegment
            && ((WaySegment) o).way == way
            && ((WaySegment) o).lowerIndex == lowerIndex;
    }

    @Override
    public int hashCode() {
        return way.hashCode() ^ lowerIndex;
    }

    @Override
    public int compareTo(WaySegment o) {
        return equals(o) ? 0 : toWay().compareTo(o.toWay());
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

    @Override
    public String toString() {
        return "WaySegment [way=" + way.getId() + ", lowerIndex=" + lowerIndex + "]";
    }
}
