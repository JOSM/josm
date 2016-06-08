// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * This class represents a state of the {@link MapView}.
 * @author Michael Zangl
 * @since 10343
 */
public class MapViewState {

    private final Projection projection = Main.getProjection();

    private final int viewWidth;
    private final int viewHeight;

    private final double scale;

    /**
     * Top left {@link EastNorth} coordinate of the view.
     */
    private final EastNorth topLeft;

    private final NavigatableComponent navigatableComponent;

    /**
     * Create a new {@link MapViewState} object for the given map view.
     * @param navigatableComponent The view.
     */
    public MapViewState(NavigatableComponent navigatableComponent) {
        this.navigatableComponent = navigatableComponent;
        viewWidth = navigatableComponent.getWidth();
        viewHeight = navigatableComponent.getHeight();

        scale = navigatableComponent.getScale();
        EastNorth center = navigatableComponent.getCenter();
        topLeft = new EastNorth(center.east() - viewWidth / 2.0 * scale, center.north() + viewHeight / 2.0 * scale);
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
     * Gets a rectangle representing the whole view area.
     * @return The rectangle.
     */
    public MapViewRectangle getViewArea() {
        return getForView(0, 0).rectTo(getForView(viewWidth, viewHeight));
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
     */
    public Projection getProjection() {
        return projection;
    }

    /**
     * A class representing a point in the map view. It allows to convert between the different coordinate systems.
     * @author Michael Zangl
     */
    public abstract class MapViewPoint {

        /**
         * Get this point in view coordinates.
         * @return The point in view coordinates.
         */
        public Point2D getInView() {
            return new Point2D.Double(getInViewX(), getInViewY());
        }

        protected abstract double getInViewX();

        protected abstract double getInViewY();

        /**
         * Convert this point to window coordinates.
         * @return The point in window coordinates.
         */
        public Point2D getInWindow() {
            Point corner = SwingUtilities.convertPoint(navigatableComponent, new Point(0, 0), null);
            return getUsingCorner(corner);
        }

        /**
         * Convert this point to screen coordinates.
         * @return The point in screen coordinates.
         */
        public Point2D getOnScreen() {
            Point corner = new Point(0, 0);
            SwingUtilities.convertPointToScreen(corner, navigatableComponent);
            return getUsingCorner(corner);
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
         * @return The positon as LatLon.
         */
        public LatLon getLatLon() {
            return projection.eastNorth2latlon(getEastNorth());
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
        protected double getInViewX() {
            return x;
        }

        @Override
        protected double getInViewY() {
            return y;
        }

        @Override
        public String toString() {
            return "MapViewViewPoint [x=" + x + ", y=" + y + "]";
        }
    }

    private class MapViewEastNorthPoint extends MapViewPoint {

        private final EastNorth eastNorth;

        MapViewEastNorthPoint(EastNorth eastNorth) {
            this.eastNorth = eastNorth;
        }

        @Override
        protected double getInViewX() {
            return (eastNorth.east() - topLeft.east()) / scale;
        }

        @Override
        protected double getInViewY() {
            return (topLeft.north() - eastNorth.north()) / scale;
        }

        @Override
        public EastNorth getEastNorth() {
            return eastNorth;
        }

        @Override
        public String toString() {
            return "MapViewEastNorthPoint [eastNorth=" + eastNorth + "]";
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
         */
        public Bounds getCornerBounds() {
            Bounds b = new Bounds(p1.getLatLon());
            b.extend(p2.getLatLon());
            return b;
        }
    }
}
