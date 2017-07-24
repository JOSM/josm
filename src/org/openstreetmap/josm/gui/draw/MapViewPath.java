// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.visitor.paint.OffsetIterator;
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
     * Gets the map view state this path is used for.
     * @return The state.
     * @since 11748
     */
    public MapViewState getMapViewState() {
        return state;
    }

    /**
     * Move the cursor to the given node.
     * @param n The node
     * @return this for easy chaining.
     */
    public MapViewPath moveTo(ILatLon n) {
        moveTo(n.getEastNorth(state.getProjecting()));
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
    public MapViewPath lineTo(ILatLon n) {
        lineTo(n.getEastNorth(state.getProjecting()));
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
    public MapViewPath shapeAround(ILatLon p1, SymbolShape symbol, double size) {
        shapeAround(p1.getEastNorth(state.getProjecting()), symbol, size);
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
    public MapViewPath append(Iterable<? extends ILatLon> nodes, boolean connect) {
        appendWay(nodes, connect, false);
        return this;
    }

    /**
     * Append a list of nodes as closed way.
     * @param nodes The nodes to append
     * @param connect <code>true</code> if we should use a lineTo as first command.
     * @return this for easy chaining.
     */
    public MapViewPath appendClosed(Iterable<? extends ILatLon> nodes, boolean connect) {
        appendWay(nodes, connect, true);
        return this;
    }

    private void appendWay(Iterable<? extends ILatLon> nodes, boolean connect, boolean close) {
        boolean useMoveTo = !connect;
        ILatLon first = null;
        for (ILatLon n : nodes) {
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
     * Converts a path in east/north coordinates to view space.
     * @param path The path
     * @since 11748
     */
    public void appendFromEastNorth(Path2D.Double path) {
        new PathVisitor() {
            @Override
            public void visitMoveTo(double x, double y) {
                moveTo(new EastNorth(x, y));
            }

            @Override
            public void visitLineTo(double x, double y) {
                lineTo(new EastNorth(x, y));
            }

            @Override
            public void visitClose() {
                closePath();
            }
        }.visit(path);
    }

    /**
     * Visits all segments of this path.
     * @param consumer The consumer to send path segments to
     * @return the total line length
     * @since 11748
     */
    public double visitLine(PathSegmentConsumer consumer) {
        LineVisitor visitor = new LineVisitor(consumer);
        visitor.visit(this);
        return visitor.inLineOffset;
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
            return visitClippedLine(length, consumer);
        } else {
            return visitClippedLine(0, consumer);
        }
    }

    /**
     * Visits all straight segments of this path. The segments are clamped to the view.
     * If they are clamped, the start points are aligned with the pattern.
     * @param strokeLength The dash pattern length. 0 to use no pattern. Only segments of this length will be removed from the line.
     * @param consumer The consumer to call for each segment
     * @return false if visiting the path failed because there e.g. were non-straight segments.
     * @since 11147
     */
    public boolean visitClippedLine(double strokeLength, PathSegmentConsumer consumer) {
        return new ClampingPathVisitor(state.getViewClipRectangle(), strokeLength, consumer)
            .visit(this);
    }

    /**
     * Gets the length of the way in visual space.
     * @return The length.
     * @since 11748
     */
    public double getLength() {
        return visitLine((inLineOffset, start, end, startIsOldEnd) -> { });
    }

    /**
     * Create a new {@link MapViewPath} that is the same as the current one except that it is offset in the view.
     * @param viewOffset The offset in view pixels
     * @return The new path
     * @since 12505
     */
    public MapViewPath offset(double viewOffset) {
        OffsetPathVisitor visitor = new OffsetPathVisitor(state, viewOffset);
        visitor.visit(this);
        return visitor.getPath();
    }

    /**
     * This class is used to visit the segments of this path.
     * @author Michael Zangl
     * @since 11147
     */
    @FunctionalInterface
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

    private interface PathVisitor {
        /**
         * Append a path to this one. The path is clipped to the current view.
         * @param path The iterator
         * @return true if adding the path was successful.
         */
        default boolean visit(Path2D.Double path) {
            double[] coords = new double[8];
            PathIterator it = path.getPathIterator(null);
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
                    // cannot handle this shape - this should be very rare and not happening in OSM draw code.
                    return false;
                }
                it.next();
            }
            return true;
        }

        void visitClose();

        void visitMoveTo(double x, double y);

        void visitLineTo(double x, double y);
    }

    private abstract class AbstractMapPathVisitor implements PathVisitor {
        private MapViewPoint lastMoveTo;

        @Override
        public void visitMoveTo(double x, double y) {
            MapViewPoint move = state.getForView(x, y);
            lastMoveTo = move;
            visitMoveTo(move);
        }

        abstract void visitMoveTo(MapViewPoint p);

        @Override
        public void visitLineTo(double x, double y) {
            visitLineTo(state.getForView(x, y));
        }

        abstract void visitLineTo(MapViewPoint p);

        @Override
        public void visitClose() {
            visitLineTo(lastMoveTo);
        }
    }

    private final class LineVisitor extends AbstractMapPathVisitor {
        private final PathSegmentConsumer consumer;
        private MapViewPoint last;
        private double inLineOffset;
        private boolean startIsOldEnd;

        LineVisitor(PathSegmentConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        void visitMoveTo(MapViewPoint p) {
            last = p;
            startIsOldEnd = false;
        }

        @Override
        void visitLineTo(MapViewPoint p) {
            consumer.addLineBetween(inLineOffset, last, p, startIsOldEnd);
            inLineOffset += last.distanceToInView(p);
            last = p;
            startIsOldEnd = true;
        }
    }

    private class ClampingPathVisitor extends AbstractMapPathVisitor {
        private final MapViewRectangle clip;
        private final PathSegmentConsumer consumer;
        protected double strokeProgress;
        private final double strokeLength;

        private MapViewPoint cursor;
        private boolean cursorIsActive;

        /**
         * Create a new {@link ClampingPathVisitor}
         * @param clip View clip rectangle
         * @param strokeLength Total length of a stroke sequence
         * @param consumer The consumer to notify of the path segments.
         */
        ClampingPathVisitor(MapViewRectangle clip, double strokeLength, PathSegmentConsumer consumer) {
            this.clip = clip;
            this.strokeLength = strokeLength;
            this.consumer = consumer;
        }

        @Override
        void visitMoveTo(MapViewPoint point) {
            cursor = point;
            cursorIsActive = false;
        }

        @Override
        void visitLineTo(MapViewPoint next) {
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
            double offset = (strokeProgress + distance) % strokeLength;
            if (offset < 0.01) {
                return entry;
            }

            return entry.interpolate(originalStart, offset / distance);
        }
    }

    private class OffsetPathVisitor extends AbstractMapPathVisitor {
        private final MapViewPath collector;
        private final ArrayList<MapViewPoint> points = new ArrayList<>();
        private final double offset;

        OffsetPathVisitor(MapViewState state, double offset) {
            this.collector = new MapViewPath(state);
            this.offset = offset;
        }

        @Override
        void visitMoveTo(MapViewPoint p) {
            finishLineSegment();
            points.add(p);
        }

        @Override
        void visitLineTo(MapViewPoint p) {
            points.add(p);
        }

        MapViewPath getPath() {
            finishLineSegment();
            return collector;
        }

        private void finishLineSegment() {
            if (points.size() > 2) {
                OffsetIterator iterator = new OffsetIterator(points, offset);
                collector.moveTo(iterator.next());
                while (iterator.hasNext()) {
                    collector.lineTo(iterator.next());
                }
                points.clear();
            }
        }
    }
}
