// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.tools.Utils;

/**
 * Iterates over a list of Way Nodes and returns screen coordinates that
 * represent a line that is shifted by a certain offset perpendicular
 * to the way direction.
 *
 * There is no intention, to handle consecutive duplicate Nodes in a
 * perfect way, but it should not throw an exception.
 * @since 11696 made public
 */
public class OffsetIterator implements Iterator<MapViewPoint> {
    private final MapViewState mapState;
    private final List<MapViewPoint> nodes;
    private final double offset;
    private int idx;

    private MapViewPoint prev;
    /* 'prev0' is a point that has distance 'offset' from 'prev' and the
     * line from 'prev' to 'prev0' is perpendicular to the way segment from
     * 'prev' to the current point.
     */
    private double xPrev0;
    private double yPrev0;

    /**
     * Creates a new offset iterator
     * @param nodes The nodes of the original line
     * @param offset The offset of the line.
     */
    public OffsetIterator(List<MapViewPoint> nodes, double offset) {
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("There must be at least 2 nodes.");
        }
        this.mapState = nodes.get(0).getMapViewState();
        this.nodes = nodes;
        this.offset = offset;
    }

    /**
     * Creates a new offset iterator
     * @param mapState The map view state this iterator is for.
     * @param nodes The nodes of the original line
     * @param offset The offset of the line.
     */
    public OffsetIterator(MapViewState mapState, List<? extends INode> nodes, double offset) {
        this.mapState = mapState;
        this.nodes = nodes.stream().filter(INode::isLatLonKnown).map(mapState::getPointFor).collect(Collectors.toList());
        this.offset = offset;
    }

    @Override
    public boolean hasNext() {
        return idx < nodes.size();
    }

    @Override
    public MapViewPoint next() {
        if (!hasNext())
            throw new NoSuchElementException();

        MapViewPoint current = getForIndex(idx);

        if (Math.abs(offset) < 0.1d) {
            idx++;
            return current;
        }

        double xCurrent = current.getInViewX();
        double yCurrent = current.getInViewY();
        if (idx == nodes.size() - 1) {
            ++idx;
            if (prev != null) {
                return mapState.getForView(xPrev0 + xCurrent - prev.getInViewX(),
                                           yPrev0 + yCurrent - prev.getInViewY());
            } else {
                return current;
            }
        }

        MapViewPoint next = getForIndex(idx + 1);
        double dxNext = next.getInViewX() - xCurrent;
        double dyNext = next.getInViewY() - yCurrent;
        double lenNext = Math.sqrt(dxNext*dxNext + dyNext*dyNext);

        if (lenNext < 1e-11) {
            lenNext = 1; // value does not matter, because dy_next and dx_next is 0
        }

        // calculate the position of the translated current point
        double om = offset / lenNext;
        double xCurrent0 = xCurrent + om * dyNext;
        double yCurrent0 = yCurrent - om * dxNext;

        if (idx == 0) {
            ++idx;
            prev = current;
            xPrev0 = xCurrent0;
            yPrev0 = yCurrent0;
            return mapState.getForView(xCurrent0, yCurrent0);
        } else {
            double dxPrev = xCurrent - prev.getInViewX();
            double dyPrev = yCurrent - prev.getInViewY();
            // determine intersection of the lines parallel to the two segments
            double det = dxNext*dyPrev - dxPrev*dyNext;
            double m = dxNext*(yCurrent0 - yPrev0) - dyNext*(xCurrent0 - xPrev0);

            if (Utils.equalsEpsilon(det, 0) || Math.signum(det) != Math.signum(m)) {
                ++idx;
                prev = current;
                xPrev0 = xCurrent0;
                yPrev0 = yCurrent0;
                return mapState.getForView(xCurrent0, yCurrent0);
            }

            double f = m / det;
            if (f < 0) {
                ++idx;
                prev = current;
                xPrev0 = xCurrent0;
                yPrev0 = yCurrent0;
                return mapState.getForView(xCurrent0, yCurrent0);
            }
            // the position of the intersection or intermittent point
            double cx = xPrev0 + f * dxPrev;
            double cy = yPrev0 + f * dyPrev;

            if (f > 1) {
                // check if the intersection point is too far away, this will happen for sharp angles
                double dxI = cx - xCurrent;
                double dyI = cy - yCurrent;
                double lenISq = dxI * dxI + dyI * dyI;

                if (lenISq > Math.abs(2 * offset * offset)) {
                    // intersection point is too far away, calculate intermittent points for capping
                    double dxPrev0 = xCurrent0 - xPrev0;
                    double dyPrev0 = yCurrent0 - yPrev0;
                    double lenPrev0 = Math.sqrt(dxPrev0 * dxPrev0 + dyPrev0 * dyPrev0);
                    f = 1 + Math.abs(offset / lenPrev0);
                    double cxCap = xPrev0 + f * dxPrev;
                    double cyCap = yPrev0 + f * dyPrev;
                    xPrev0 = cxCap;
                    yPrev0 = cyCap;
                    // calculate a virtual prev point which lies on a line that goes through current and
                    // is perpendicular to the line that goes through current and the intersection
                    // so that the next capping point is calculated with it.
                    double lenI = Math.sqrt(lenISq);
                    double xv = xCurrent + dyI / lenI;
                    double yv = yCurrent - dxI / lenI;

                    prev = mapState.getForView(xv, yv);
                    return mapState.getForView(cxCap, cyCap);
                }
            }
            ++idx;
            prev = current;
            xPrev0 = xCurrent0;
            yPrev0 = yCurrent0;
            return mapState.getForView(cx, cy);
        }
    }

    private MapViewPoint getForIndex(int i) {
        return nodes.get(i);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
