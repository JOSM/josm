// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Container;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComponent;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This class represents a state of the {@link MapView}.
 * @author Michael Zangl
 * @since 10343
 */
public final class MapViewState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A flag indicating that the point is outside to the top of the map view.
     * @since 10827
     */
    public static final int OUTSIDE_TOP = 1;

    /**
     * A flag indicating that the point is outside to the bottom of the map view.
     * @since 10827
     */
    public static final int OUTSIDE_BOTTOM = 2;

    /**
     * A flag indicating that the point is outside to the left of the map view.
     * @since 10827
     */
    public static final int OUTSIDE_LEFT = 4;

    /**
     * A flag indicating that the point is outside to the right of the map view.
     * @since 10827
     */
    public static final int OUTSIDE_RIGHT = 8;

    /**
     * Additional pixels outside the view for where to start clipping.
     */
    private static final int CLIP_BOUNDS = 50;

    private final transient Projecting projecting;

    private final int viewWidth;
    private final int viewHeight;

    private final double scale;

    /**
     * Top left {@link EastNorth} coordinate of the view.
     */
    private final EastNorth topLeft;

    private final Point topLeftOnScreen;
    private final Point topLeftInWindow;

    /**
     * Create a new {@link MapViewState}
     * @param projection The projection to use.
     * @param viewWidth The view width
     * @param viewHeight The view height
     * @param scale The scale to use
     * @param topLeft The top left corner in east/north space.
     * @param topLeftInWindow The top left point in window
     * @param topLeftOnScreen The top left point on screen
     */
    private MapViewState(Projecting projection, int viewWidth, int viewHeight, double scale, EastNorth topLeft,
            Point topLeftInWindow, Point topLeftOnScreen) {
        CheckParameterUtil.ensureParameterNotNull(projection, "projection");
        CheckParameterUtil.ensureParameterNotNull(topLeft, "topLeft");
        CheckParameterUtil.ensureParameterNotNull(topLeftInWindow, "topLeftInWindow");
        CheckParameterUtil.ensureParameterNotNull(topLeftOnScreen, "topLeftOnScreen");

        this.projecting = projection;
        this.scale = scale;
        this.topLeft = topLeft;

        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.topLeftInWindow = topLeftInWindow;
        this.topLeftOnScreen = topLeftOnScreen;
    }

    private MapViewState(Projecting projection, int viewWidth, int viewHeight, double scale, EastNorth topLeft) {
        this(projection, viewWidth, viewHeight, scale, topLeft, new Point(0, 0), new Point(0, 0));
    }

    private MapViewState(EastNorth topLeft, MapViewState mvs) {
        this(mvs.projecting, mvs.viewWidth, mvs.viewHeight, mvs.scale, topLeft, mvs.topLeftInWindow, mvs.topLeftOnScreen);
    }

    private MapViewState(double scale, MapViewState mvs) {
        this(mvs.projecting, mvs.viewWidth, mvs.viewHeight, scale, mvs.topLeft, mvs.topLeftInWindow, mvs.topLeftOnScreen);
    }

    private MapViewState(JComponent position, MapViewState mvs) {
        this(mvs.projecting, position.getWidth(), position.getHeight(), mvs.scale, mvs.topLeft,
                findTopLeftInWindow(position), findTopLeftOnScreen(position));
    }

    private MapViewState(Projecting projecting, MapViewState mvs) {
        this(projecting, mvs.viewWidth, mvs.viewHeight, mvs.scale, mvs.topLeft, mvs.topLeftInWindow, mvs.topLeftOnScreen);
    }

    /**
     * This is visible for JMockit.
     *
     * @param position The component to get the top left position of its window
     * @return the top left point in window
     */
    static Point findTopLeftInWindow(JComponent position) {
        Point result = new Point();
        // better than using swing utils, since this allows us to use the method if no screen is present.
        Container component = position;
        while (component != null) {
            result.x += component.getX();
            result.y += component.getY();
            component = component.getParent();
        }
        return result;
    }

    /**
     * This is visible for JMockit.
     *
     * @param position The component to get the top left position of its screen
     * @return the top left point on screen
     */
    static Point findTopLeftOnScreen(JComponent position) {
        try {
            return position.getLocationOnScreen();
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("position", position).put("parent", position::getParent);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + " [projecting=" + this.projecting
            + " viewWidth=" + this.viewWidth
            + " viewHeight=" + this.viewHeight
            + " scale=" + this.scale
            + " topLeft=" + this.topLeft + ']';
    }

    /**
     * The scale in east/north units per pixel.
     * @return The scale.
     */
    public double getScale() {
        return scale;
    }

    /**
     * Gets the MapViewPoint representation for a position in view coordinates.
     * @param x The x coordinate inside the view.
     * @param y The y coordinate inside the view.
     * @return The MapViewPoint.
     */
    public MapViewPoint getForView(double x, double y) {
        return new MapViewViewPoint(x, y);
    }

    /**
     * Gets the {@link MapViewPoint} for the given {@link EastNorth} coordinate.
     * @param eastNorth the position.
     * @return The point for that position.
     */
    public MapViewPoint getPointFor(EastNorth eastNorth) {
        return new MapViewEastNorthPoint(eastNorth);
    }

    /**
     * Gets the {@link MapViewPoint} for the given {@link LatLon} coordinate.
     * <p>
     * This method exists to not break binary compatibility with old plugins
     * @param latlon the position
     * @return The point for that position.
     * @since 10651
     */
    public MapViewPoint getPointFor(LatLon latlon) {
        return getPointFor((ILatLon) latlon);
    }

    /**
     * Gets the {@link MapViewPoint} for the given {@link LatLon} coordinate.
     * @param latlon the position
     * @return The point for that position.
     * @since 12161
     */
    public MapViewPoint getPointFor(ILatLon latlon) {
        try {
            return getPointFor(Optional.ofNullable(latlon.getEastNorth(getProjection()))
                    .orElseThrow(IllegalArgumentException::new));
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("latlon", latlon);
        }
    }

    /**
     * Gets the {@link MapViewPoint} for the given node.
     * This is faster than {@link #getPointFor(LatLon)} because it uses the node east/north cache.
     * @param node The node
     * @return The position of that node.
     * @since 10827
     */
    public MapViewPoint getPointFor(Node node) {
        return getPointFor((ILatLon) node);
    }

    /**
     * Gets a rectangle representing the whole view area.
     * @return The rectangle.
     */
    public MapViewRectangle getViewArea() {
        return getForView(0, 0).rectTo(getForView(viewWidth, viewHeight));
    }

    /**
     * Gets a rectangle of the view as map view area.
     * @param rectangle The rectangle to get.
     * @return The view area.
     * @since 10827
     */
    public MapViewRectangle getViewArea(Rectangle2D rectangle) {
        return getForView(rectangle.getMinX(), rectangle.getMinY()).rectTo(getForView(rectangle.getMaxX(), rectangle.getMaxY()));
    }

    /**
     * Gets the center of the view.
     * @return The center position.
     */
    public MapViewPoint getCenter() {
        return getForView(viewWidth / 2.0, viewHeight / 2.0);
    }

    /**
     * Gets the width of the view on the Screen;
     * @return The width of the view component in screen pixel.
     */
    public double getViewWidth() {
        return viewWidth;
    }

    /**
     * Gets the height of the view on the Screen;
     * @return The height of the view component in screen pixel.
     */
    public double getViewHeight() {
        return viewHeight;
    }

    /**
     * Gets the current projection used for the MapView.
     * @return The projection.
     * @see #getProjecting()
     */
    public Projection getProjection() {
        return projecting.getBaseProjection();
    }

    /**
     * Gets the current projecting instance that is used to convert between east/north and lat/lon space.
     * @return The projection.
     * @since 12161
     */
    public Projecting getProjecting() {
        return projecting;
    }

    /**
     * Creates an affine transform that is used to convert the east/north coordinates to view coordinates.
     * @return The affine transform. It should not be changed.
     * @since 10375
     */
    public AffineTransform getAffineTransform() {
        return new AffineTransform(1.0 / scale, 0.0, 0.0, -1.0 / scale, -topLeft.east() / scale,
                topLeft.north() / scale);
    }

    /**
     * Gets a rectangle that is several pixel bigger than the view. It is used to define the view clipping.
     * @return The rectangle.
     */
    public MapViewRectangle getViewClipRectangle() {
        return getForView(-CLIP_BOUNDS, -CLIP_BOUNDS).rectTo(getForView(getViewWidth() + CLIP_BOUNDS, getViewHeight() + CLIP_BOUNDS));
    }

    /**
     * Returns the area for the given bounds.
     * @param bounds bounds
     * @return the area for the given bounds
     */
    public Area getArea(Bounds bounds) {
        Path2D area = new Path2D.Double();
        getProjection().visitOutline(bounds, en -> {
            MapViewPoint point = getPointFor(en);
            if (area.getCurrentPoint() == null) {
                area.moveTo(point.getInViewX(), point.getInViewY());
            } else {
                area.lineTo(point.getInViewX(), point.getInViewY());
            }
        });
        area.closePath();
        return new Area(area);
    }

    /**
     * Creates a new state that is the same as the current state except for that it is using a new center.
     * @param newCenter The new center coordinate.
     * @return The new state.
     * @since 10375
     */
    public MapViewState usingCenter(EastNorth newCenter) {
        return movedTo(getCenter(), newCenter);
    }

    /**
     * Creates a new state that is moved to an east/north coordinate.
     * @param mapViewPoint The reference point.
     * @param newEastNorthThere The east/north coordinate that should be there.
     * @return The new state.
     * @since 10375
     */
    public MapViewState movedTo(MapViewPoint mapViewPoint, EastNorth newEastNorthThere) {
        EastNorth delta = newEastNorthThere.subtract(mapViewPoint.getEastNorth());
        if (delta.distanceSq(0, 0) < .1e-20) {
            return this;
        } else {
            return new MapViewState(topLeft.add(delta), this);
        }
    }

    /**
     * Creates a new state that is the same as the current state except for that it is using a new scale.
     * @param newScale The new scale to use.
     * @return The new state.
     * @since 10375
     */
    public MapViewState usingScale(double newScale) {
        return new MapViewState(newScale, this);
    }

    /**
     * Creates a new state that is the same as the current state except for that it is using the location of the given component.
     * <p>
     * The view is moved so that the center is the same as the old center.
     * @param position The new location to use.
     * @return The new state.
     * @since 10375
     */
    public MapViewState usingLocation(JComponent position) {
        EastNorth center = this.getCenter().getEastNorth();
        return new MapViewState(position, this).usingCenter(center);
    }

    /**
     * Creates a state that uses the projection.
     * @param projection The projection to use.
     * @return The new state.
     * @since 10486
     */
    public MapViewState usingProjection(Projection projection) {
        if (projection.equals(this.projecting)) {
            return this;
        } else {
            return new MapViewState(projection, this);
        }
    }

    /**
     * Create the default {@link MapViewState} object for the given map view. The screen position won't be set so that this method can be used
     * before the view was added to the hierarchy.
     * @param width The view width
     * @param height The view height
     * @return The state
     * @since 10375
     */
    public static MapViewState createDefaultState(int width, int height) {
        Projection projection = ProjectionRegistry.getProjection();
        double scale = projection.getDefaultZoomInPPD();
        MapViewState state = new MapViewState(projection, width, height, scale, new EastNorth(0, 0));
        EastNorth center = calculateDefaultCenter();
        return state.movedTo(state.getCenter(), center);
    }

    private static EastNorth calculateDefaultCenter() {
        Bounds b = Optional.ofNullable(DownloadDialog.getSavedDownloadBounds()).orElseGet(
                () -> ProjectionRegistry.getProjection().getWorldBoundsLatLon());
        return b.getCenter().getEastNorth(ProjectionRegistry.getProjection());
    }

    /**
     * Check if this MapViewState equals another one, disregarding the position
     * of the JOSM window on screen.
     * @param other the other MapViewState
     * @return true if the other MapViewState has the same size, scale, position and projection,
     * false otherwise
     */
    public boolean equalsInWindow(MapViewState other) {
        return other != null &&
                this.viewWidth == other.viewWidth &&
                this.viewHeight == other.viewHeight &&
                this.scale == other.scale &&
                Objects.equals(this.topLeft, other.topLeft) &&
                Objects.equals(this.projecting, other.projecting);
    }

    /**
     * A class representing a point in the map view. It allows to convert between the different coordinate systems.
     * @author Michael Zangl
     */
    public abstract class MapViewPoint {
        /**
         * Gets the map view state this path is used for.
         * @return The state.
         * @since 12505
         */
        public MapViewState getMapViewState() {
            return MapViewState.this;
        }

        /**
         * Get this point in view coordinates.
         * @return The point in view coordinates.
         */
        public Point2D getInView() {
            return new Point2D.Double(getInViewX(), getInViewY());
        }

        /**
         * Get the x coordinate in view space without creating an intermediate object.
         * @return The x coordinate
         * @since 10827
         */
        public abstract double getInViewX();

        /**
         * Get the y coordinate in view space without creating an intermediate object.
         * @return The y coordinate
         * @since 10827
         */
        public abstract double getInViewY();

        /**
         * Convert this point to window coordinates.
         * @return The point in window coordinates.
         */
        public Point2D getInWindow() {
            return getUsingCorner(topLeftInWindow);
        }

        /**
         * Convert this point to screen coordinates.
         * @return The point in screen coordinates.
         */
        public Point2D getOnScreen() {
            return getUsingCorner(topLeftOnScreen);
        }

        private Double getUsingCorner(Point corner) {
            return new Point2D.Double(corner.getX() + getInViewX(), corner.getY() + getInViewY());
        }

        /**
         * Gets the {@link EastNorth} coordinate of this point.
         * @return The east/north coordinate.
         */
        public EastNorth getEastNorth() {
            return new EastNorth(topLeft.east() + getInViewX() * scale, topLeft.north() - getInViewY() * scale);
        }

        /**
         * Create a rectangle from this to the other point.
         * @param other The other point. Needs to be of the same {@link MapViewState}
         * @return A rectangle.
         */
        public MapViewRectangle rectTo(MapViewPoint other) {
            return new MapViewRectangle(this, other);
        }

        /**
         * Gets the current position in LatLon coordinates according to the current projection.
         * @return The position as LatLon.
         * @see #getLatLonClamped()
         */
        public LatLon getLatLon() {
            return projecting.getBaseProjection().eastNorth2latlon(getEastNorth());
        }

        /**
         * Gets the latlon coordinate clamped to the current world area.
         * @return The lat/lon coordinate
         * @since 10805
         */
        public LatLon getLatLonClamped() {
            return projecting.eastNorth2latlonClamped(getEastNorth());
        }

        /**
         * Add the given offset to this point
         * @param en The offset in east/north space.
         * @return The new point
         * @since 10651
         */
        public MapViewPoint add(EastNorth en) {
            return new MapViewEastNorthPoint(getEastNorth().add(en));
        }

        /**
         * Check if this point is inside the view bounds.
         *
         * This is the case iff <code>getOutsideRectangleFlags(getViewArea())</code> returns no flags
         * @return true if it is.
         * @since 10827
         */
        public boolean isInView() {
            return inRange(getInViewX(), 0, getViewWidth()) && inRange(getInViewY(), 0, getViewHeight());
        }

        private boolean inRange(double val, int min, double max) {
            return val >= min && val < max;
        }

        /**
         * Gets the direction in which this point is outside of the given view rectangle.
         * @param rect The rectangle to check against.
         * @return The direction in which it is outside of the view, as OUTSIDE_... flags.
         * @since 10827
         */
        public int getOutsideRectangleFlags(MapViewRectangle rect) {
            Rectangle2D bounds = rect.getInView();
            int flags = 0;
            if (getInViewX() < bounds.getMinX()) {
                flags |= OUTSIDE_LEFT;
            } else if (getInViewX() > bounds.getMaxX()) {
                flags |= OUTSIDE_RIGHT;
            }
            if (getInViewY() < bounds.getMinY()) {
                flags |= OUTSIDE_TOP;
            } else if (getInViewY() > bounds.getMaxY()) {
                flags |= OUTSIDE_BOTTOM;
            }

            return flags;
        }

        /**
         * Gets the sum of the x/y view distances between the points. |x1 - x2| + |y1 - y2|
         * @param p2 The other point
         * @return The norm
         * @since 10827
         */
        public double oneNormInView(MapViewPoint p2) {
            return Math.abs(getInViewX() - p2.getInViewX()) + Math.abs(getInViewY() - p2.getInViewY());
        }

        /**
         * Gets the squared distance between this point and an other point.
         * @param p2 The other point
         * @return The squared distance.
         * @since 10827
         */
        public double distanceToInViewSq(MapViewPoint p2) {
            double dx = getInViewX() - p2.getInViewX();
            double dy = getInViewY() - p2.getInViewY();
            return dx * dx + dy * dy;
        }

        /**
         * Gets the distance between this point and an other point.
         * @param p2 The other point
         * @return The distance.
         * @since 10827
         */
        public double distanceToInView(MapViewPoint p2) {
            return Math.sqrt(distanceToInViewSq(p2));
        }

        /**
         * Do a linear interpolation to the other point
         * @param p1 The other point
         * @param i The interpolation factor. 0 is at the current point, 1 at the other point.
         * @return The new point
         * @since 10874
         */
        public MapViewPoint interpolate(MapViewPoint p1, double i) {
            return new MapViewViewPoint((1 - i) * getInViewX() + i * p1.getInViewX(), (1 - i) * getInViewY() + i * p1.getInViewY());
        }
    }

    private class MapViewViewPoint extends MapViewPoint {
        private final double x;
        private final double y;

        MapViewViewPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public double getInViewX() {
            return x;
        }

        @Override
        public double getInViewY() {
            return y;
        }

        @Override
        public String toString() {
            return "MapViewViewPoint [x=" + x + ", y=" + y + ']';
        }
    }

    private class MapViewEastNorthPoint extends MapViewPoint {

        private final EastNorth eastNorth;

        MapViewEastNorthPoint(EastNorth eastNorth) {
            this.eastNorth = Objects.requireNonNull(eastNorth, "eastNorth");
        }

        @Override
        public double getInViewX() {
            return (eastNorth.east() - topLeft.east()) / scale;
        }

        @Override
        public double getInViewY() {
            return (topLeft.north() - eastNorth.north()) / scale;
        }

        @Override
        public EastNorth getEastNorth() {
            return eastNorth;
        }

        @Override
        public String toString() {
            return "MapViewEastNorthPoint [eastNorth=" + eastNorth + ']';
        }
    }

    /**
     * A rectangle on the MapView. It is rectangular in screen / EastNorth space.
     * @author Michael Zangl
     */
    public class MapViewRectangle {
        private final MapViewPoint p1;
        private final MapViewPoint p2;

        /**
         * Create a new MapViewRectangle
         * @param p1 The first point to use
         * @param p2 The second point to use.
         */
        MapViewRectangle(MapViewPoint p1, MapViewPoint p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        /**
         * Gets the projection bounds for this rectangle.
         * @return The projection bounds.
         */
        public ProjectionBounds getProjectionBounds() {
            ProjectionBounds b = new ProjectionBounds(p1.getEastNorth());
            b.extend(p2.getEastNorth());
            return b;
        }

        /**
         * Gets a rough estimate of the bounds by assuming lat/lon are parallel to x/y.
         * @return The bounds computed by converting the corners of this rectangle.
         * @see #getLatLonBoundsBox()
         */
        public Bounds getCornerBounds() {
            Bounds b = new Bounds(p1.getLatLon());
            b.extend(p2.getLatLon());
            return b;
        }

        /**
         * Gets the real bounds that enclose this rectangle.
         * This is computed respecting that the borders of this rectangle may not be a straignt line in latlon coordinates.
         * @return The bounds.
         * @since 10458
         */
        public Bounds getLatLonBoundsBox() {
            // TODO @michael2402: Use hillclimb.
            return projecting.getBaseProjection().getLatLonBoundsBox(getProjectionBounds());
        }

        /**
         * Gets this rectangle on the screen.
         * @return The rectangle.
         * @since 10651
         */
        public Rectangle2D getInView() {
            double x1 = p1.getInViewX();
            double y1 = p1.getInViewY();
            double x2 = p2.getInViewX();
            double y2 = p2.getInViewY();
            return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
        }

        /**
         * Check if the rectangle intersects the map view area.
         * @return <code>true</code> if it intersects.
         * @since 10827
         */
        public boolean isInView() {
            return getInView().intersects(getViewArea().getInView());
        }

        /**
         * Gets the entry point at which a line between start and end enters the current view.
         * @param start The start
         * @param end The end
         * @return The entry point or <code>null</code> if the line does not intersect this view.
         */
        public MapViewPoint getLineEntry(MapViewPoint start, MapViewPoint end) {
            ProjectionBounds bounds = getProjectionBounds();
            EastNorth enStart = start.getEastNorth();
            if (bounds.contains(enStart)) {
                return start;
            }

            EastNorth enEnd = end.getEastNorth();
            double dx = enEnd.east() - enStart.east();
            double boundX = dx > 0 ? bounds.minEast : bounds.maxEast;
            EastNorth borderIntersection = Geometry.getSegmentSegmentIntersection(enStart, enEnd,
                    new EastNorth(boundX, bounds.minNorth),
                    new EastNorth(boundX, bounds.maxNorth));
            if (borderIntersection != null) {
                return getPointFor(borderIntersection);
            }

            double dy = enEnd.north() - enStart.north();
            double boundY = dy > 0 ? bounds.minNorth : bounds.maxNorth;
            borderIntersection = Geometry.getSegmentSegmentIntersection(enStart, enEnd,
                    new EastNorth(bounds.minEast, boundY),
                    new EastNorth(bounds.maxEast, boundY));
            if (borderIntersection != null) {
                return getPointFor(borderIntersection);
            }

            return null;
        }

        @Override
        public String toString() {
            return "MapViewRectangle [p1=" + p1 + ", p2=" + p2 + ']';
        }
    }
}
