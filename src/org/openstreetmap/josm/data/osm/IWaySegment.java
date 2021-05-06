// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.awt.geom.Line2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

import org.openstreetmap.josm.tools.Logging;

/**
 * A segment consisting of 2 consecutive nodes out of a way.
 * @author Taylor Smock
 * @param <N> The node type
 * @param <W> The way type
 * @since xxx
 */
public class IWaySegment<N extends INode, W extends IWay<N>> implements Comparable<IWaySegment<N, W>> {

    /**
     * The way.
     */
    public final W way;

    /**
     * The index of one of the 2 nodes in the way.  The other node has the
     * index <code>lowerIndex + 1</code>.
     */
    public final int lowerIndex;

    /**
     * Constructs a new {@code IWaySegment}.
     * @param w The way
     * @param i The node lower index
     * @throws IllegalArgumentException in case of invalid index
     */
    public IWaySegment(W w, int i) {
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
    public N getFirstNode() {
        return way.getNode(lowerIndex);
    }

    /**
     * Returns the second (last) node of the way segment.
     * @return the second node
     */
    public N getSecondNode() {
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
    public static <N extends INode, W extends IWay<N>> IWaySegment<N, W> forNodePair(W way, N first, N second) {
        int endIndex = way.getNodesCount() - 1;
        while (endIndex > 0) {
            final int indexOfFirst = way.getNodes().subList(0, endIndex).lastIndexOf(first);
            if (second.equals(way.getNode(indexOfFirst + 1))) {
                return new IWaySegment<>(way, indexOfFirst);
            }
            endIndex--;
        }
        throw new IllegalArgumentException("Node pair is not part of way!");
    }

    /**
     * Returns this way segment as complete way.
     * @return the way segment as {@code Way}
     * @throws IllegalAccessException See {@link Constructor#newInstance}
     * @throws IllegalArgumentException See {@link Constructor#newInstance}
     * @throws InstantiationException See {@link Constructor#newInstance}
     * @throws InvocationTargetException See {@link Constructor#newInstance}
     * @throws NoSuchMethodException See {@link Class#getConstructor}
     */
    public W toWay()
      throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        // If the number of nodes is 2, then don't bother creating a new way
        if (this.way.getNodes().size() == 2) {
            return this.way;
        }
        // Since the way determines the generic class, this.way.getClass() is always Class<W>, assuming
        // that way remains the defining element for the type, and remains final.
        @SuppressWarnings("unchecked")
        Class<W> clazz = (Class<W>) this.way.getClass();
        Constructor<W> constructor;
        W w;
        try {
            // Check for clone constructor
            constructor = clazz.getConstructor(clazz);
            w = constructor.newInstance(this.way);
        } catch (NoSuchMethodException e) {
            Logging.trace(e);
            constructor = clazz.getConstructor();
            w = constructor.newInstance();
        }

        w.setNodes(Arrays.asList(getFirstNode(), getSecondNode()));
        return w;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IWaySegment<?, ?> that = (IWaySegment<?, ?>) o;
        return lowerIndex == that.lowerIndex &&
          Objects.equals(way, that.way);
    }

    @Override
    public int hashCode() {
        return Objects.hash(way, lowerIndex);
    }

    @Override
    public int compareTo(IWaySegment o) {
        final W thisWay;
        final IWay<?> otherWay;
        try {
            thisWay = toWay();
            otherWay = o == null ? null : o.toWay();
        } catch (ReflectiveOperationException e) {
            Logging.error(e);
            return -1;
        }
        return o == null ? -1 : (equals(o) ? 0 : thisWay.compareTo(otherWay));
    }

    /**
     * Checks whether this segment crosses other segment
     *
     * @param s2 The other segment
     * @return true if both segments crosses
     */
    public boolean intersects(IWaySegment<?, ?> s2) {
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
    public boolean isSimilar(IWaySegment<?, ?> s2) {
        return (getFirstNode().equals(s2.getFirstNode()) && getSecondNode().equals(s2.getSecondNode()))
          || (getFirstNode().equals(s2.getSecondNode()) && getSecondNode().equals(s2.getFirstNode()));
    }

    @Override
    public String toString() {
        return "IWaySegment [way=" + way.getUniqueId() + ", lowerIndex=" + lowerIndex + ']';
    }
}
