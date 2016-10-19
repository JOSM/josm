// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.PathIterator;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;


/**
 * This is a version of a java Path2D that allows you to add points to it by simply giving their east/north, lat/lon or node coordinates.
 * <p>
 * It is possible to clip the part of the path that is outside the view. This is useful when drawing dashed lines. Those lines use up a lot of
 * performance if the zoom level is high and the part outside the view is long. See {@link #computeClippedLine(Stroke)}.
 * @author Michael Zangl
 * @since 10875
 */
public class MapViewPath extends MapPath2D {

    private final MapViewState state;

    /**
     * Create a new path
     * @param mv The map view to use for coordinate conversion.
     */
    public MapViewPath(MapView mv) {
        this(mv.getState());
    }

    /**
     * Create a new path
     * @param state The state to use for coordinate conversion.
     */
    public MapViewPath(MapViewState state) {
        this.state = state;
    }

    /**
     * Move the cursor to the given node.
     * @param n The node
     * @return this for easy chaining.
     */
    public MapViewPath moveTo(Node n) {
        moveTo(n.getEastNorth());
        return this;
    }

    /**
     * Move the cursor to the given position.
     * @param eastNorth The position
     * @return this for easy chaining.
     */
    public MapViewPath moveTo(EastNorth eastNorth) {
        moveTo(state.getPointFor(eastNorth));
        return this;
    }

    @Override
    public MapViewPath moveTo(MapViewPoint p) {
        super.moveTo(p);
        return this;
    }

    /**
     * Draw a line to the node.
     * <p>
     * line clamping to view is done automatically.
     * @param n The node
     * @return this for easy chaining.
     */
    public MapViewPath lineTo(Node n) {
        lineTo(n.getEastNorth());
        return this;
    }

    /**
     * Draw a line to the position.
     * <p>
     * line clamping to view is done automatically.
     * @param eastNorth The position
     * @return this for easy chaining.
     */
    public MapViewPath lineTo(EastNorth eastNorth) {
        lineTo(state.getPointFor(eastNorth));
        return this;
    }

    @Override
    public MapViewPath lineTo(MapViewPoint p) {
        super.lineTo(p);
        return this;
    }

    /**
     * Add the given shape centered around the current node.
     * @param p1 The point to draw around
     * @param symbol The symbol type
     * @param size The size of the symbol in pixel
     * @return this for easy chaining.
     */
    public MapViewPath shapeAround(Node p1, SymbolShape symbol, double size) {
        shapeAround(p1.getEastNorth(), symbol, size);
        return this;
    }

    /**
     * Add the given shape centered around the current position.
     * @param eastNorth The point to draw around
     * @param symbol The symbol type
     * @param size The size of the symbol in pixel
     * @return this for easy chaining.
     */
    public MapViewPath shapeAround(EastNorth eastNorth, SymbolShape symbol, double size) {
        shapeAround(state.getPointFor(eastNorth), symbol, size);
        return this;
    }

    @Override
    public MapViewPath shapeAround(MapViewPoint p, SymbolShape symbol, double size) {
        super.shapeAround(p, symbol, size);
        return this;
    }

    /**
     * Append a list of nodes
     * @param nodes The nodes to append
     * @param connect <code>true</code> if we should use a lineTo as first command.
     * @return this for easy chaining.
     */
    public MapViewPath append(Iterable<Node> nodes, boolean connect) {
        appendWay(nodes, connect, false);
        return this;
    }

    /**
     * Append a list of nodes as closed way.
     * @param nodes The nodes to append
     * @param connect <code>true</code> if we should use a lineTo as first command.
     * @return this for easy chaining.
     */
    public MapViewPath appendClosed(Iterable<Node> nodes, boolean connect) {
        appendWay(nodes, connect, true);
        return this;
    }

    private void appendWay(Iterable<Node> nodes, boolean connect, boolean close) {
        boolean useMoveTo = !connect;
        Node first = null;
        for (Node n : nodes) {
            if (useMoveTo) {
                moveTo(n);
            } else {
                lineTo(n);
            }
            if (close && first == null) {
                first = n;
            }
            useMoveTo = false;
        }
        if (first != null) {
            lineTo(first);
        }
    }

    /**
     * Compute a line that is similar to the current path expect for that parts outside the screen are skipped using moveTo commands.
     *
     * The line is computed in a way that dashes stay in their place when moving the view.
     *
     * The resulting line is not intended to fill areas.
     * @param stroke The stroke to compute the line for.
     * @return The new line shape.
     * @since 11147
     */
    public Shape computeClippedLine(Stroke stroke) {
        MapPath2D clamped = new MapPath2D();
        if (visitClippedLine(stroke, (inLineOffset, start, end, startIsOldEnd) -> {
            if (!startIsOldEnd) {
                clamped.moveTo(start);
            }
            clamped.lineTo(end);
        })) {
            return clamped;
        } else {
            // could not clip the path.
            return this;
        }
    }

    /**
     * Visits all straight segments of this path. The segments are clamped to the view.
     * If they are clamped, the start points are aligned with the pattern.
     * @param stroke The stroke to take the dash information from.
     * @param consumer The consumer to call for each segment
     * @return false if visiting the path failed because there e.g. were non-straight segments.
     * @since 11147
     */
    public boolean visitClippedLine(Stroke stroke, PathSegmentConsumer consumer) {
        if (stroke instanceof BasicStroke && ((BasicStroke) stroke).getDashArray() != null) {
            float length = 0;
            for (float f : ((BasicStroke) stroke).getDashArray()) {
                length += f;
            }
            return visitClippedLine(((BasicStroke) stroke).getDashPhase(), length, consumer);
        } else {
            return visitClippedLine(0, 0, consumer);
        }
    }

    /**
     * Visits all straight segments of this path. The segments are clamped to the view.
     * If they are clamped, the start points are aligned with the pattern.
     * @param strokeOffset The initial offset of the pattern
     * @param strokeLength The dash pattern length. 0 to use no pattern.
     * @param consumer The consumer to call for each segment
     * @return false if visiting the path failed because there e.g. were non-straight segments.
     * @since 11147
     */
    public boolean visitClippedLine(double strokeOffset, double strokeLength, PathSegmentConsumer consumer) {
        return new ClampingPathVisitor(state.getViewClipRectangle(), strokeOffset, strokeLength, consumer)
            .visit(this);
    }


    /**
     * This class is used to visit the segments of this path.
     * @author Michael Zangl
     * @since 11147
     */
    public interface PathSegmentConsumer {

        /**
         * Add a line segment between two points
         * @param inLineOffset The offset of start in the line
         * @param start The start point
         * @param end The end point
         * @param startIsOldEnd If the start point equals the last end point.
         */
        void addLineBetween(double inLineOffset, MapViewPoint start, MapViewPoint end, boolean startIsOldEnd);

    }

    private class ClampingPathVisitor {
        private final MapViewRectangle clip;
        private final PathSegmentConsumer consumer;
        protected double strokeProgress;
        private final double strokeLength;
        private MapViewPoint lastMoveTo;

        private MapViewPoint cursor;
        private boolean cursorIsActive = false;

        /**
         * Create a new {@link ClampingPathVisitor}
         * @param clip View clip rectangle
         * @param strokeOffset Initial stroke offset
         * @param strokeLength Total length of a stroke sequence
         * @param consumer The consumer to notify of the path segments.
         */
        ClampingPathVisitor(MapViewRectangle clip, double strokeOffset, double strokeLength, PathSegmentConsumer consumer) {
            this.clip = clip;
            this.strokeProgress = Math.min(strokeLength - strokeOffset, 0);
            this.strokeLength = strokeLength;
            this.consumer = consumer;
        }

        /**
         * Append a path to this one. The path is clipped to the current view.
         * @param mapViewPath The iterator
         * @return true if adding the path was successful.
         */
        public boolean visit(MapViewPath mapViewPath) {
            double[] coords = new double[8];
            PathIterator it = mapViewPath.getPathIterator(null);
            while (!it.isDone()) {
                int type = it.currentSegment(coords);
                switch (type) {
                case PathIterator.SEG_CLOSE:
                    visitClose();
                    break;
                case PathIterator.SEG_LINETO:
                    visitLineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_MOVETO:
                    visitMoveTo(coords[0], coords[1]);
                    break;
                default:
                    // cannot handle this shape - this should be very rare. We let Java2D do the clipping.
                    return false;
                }
                it.next();
            }
            return true;
        }

        void visitClose() {
            drawLineTo(lastMoveTo);
        }

        void visitMoveTo(double x, double y) {
            MapViewPoint point = state.getForView(x, y);
            lastMoveTo = point;
            cursor = point;
            cursorIsActive = false;
        }

        void visitLineTo(double x, double y) {
            drawLineTo(state.getForView(x, y));
        }

        private void drawLineTo(MapViewPoint next) {
            MapViewPoint entry = clip.getLineEntry(cursor, next);
            if (entry != null) {
                MapViewPoint exit = clip.getLineEntry(next, cursor);
                if (!cursorIsActive || !entry.equals(cursor)) {
                    entry = alignStrokeOffset(entry, cursor);
                }
                consumer.addLineBetween(strokeProgress + cursor.distanceToInView(entry), entry, exit, cursorIsActive);
                cursorIsActive = exit.equals(next);
            }
            strokeProgress += cursor.distanceToInView(next);

            cursor = next;
        }


        private MapViewPoint alignStrokeOffset(MapViewPoint entry, MapViewPoint originalStart) {
            double distanceSq = entry.distanceToInViewSq(originalStart);
            if (distanceSq < 0.01 || strokeLength <= 0.001) {
                // don't move if there is nothing to move.
                return entry;
            }

            double distance = Math.sqrt(distanceSq);
            double offset = ((strokeProgress + distance)) % strokeLength;
            if (offset < 0.01) {
                return entry;
            }

            return entry.interpolate(originalStart, offset / distance);
        }

    }
}
