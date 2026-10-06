// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.EnumProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.draw.BlendComposite;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.Logging;

/**
 * A grid drawn over the whole map view, on top of all layers.
 * <p>
 * The grid is a pure visual aid (there is no snapping to it). It is either aligned to latitude/longitude, with a
 * spacing in degrees, or to the projected coordinates, with a spacing in metres (true distance, measured at the
 * grid origin), an optional rotation and an origin offset. When the grid cells would become
 * smaller than a minimal size on screen, the spacing is multiplied by 10 until the cells are large enough, so the
 * grid stays readable at every zoom level while remaining aligned to the configured one.
 * <p>
 * All settings are preferences (prefix {@code draw.grid.}), see {@link org.openstreetmap.josm.gui.preferences.display.GridPreference}.
 * <p>
 * An instance registers itself as a preference listener, so {@link #destroy()} must be called when it is no
 * longer used ({@link org.openstreetmap.josm.gui.MapFrame} does this), otherwise the listener is leaked.
 * @since 19640
 */
public class MapGridPaintable extends AbstractMapViewPaintable implements PreferenceChangedListener, Destroyable {

    /**
     * The kind of coordinates a grid is aligned to.
     */
    public enum GridType {
        /** lines of constant latitude and longitude, spacing in degrees */
        LATLON,
        /** lines of constant projected east/north coordinate (optionally rotated), spacing in metres */
        PROJECTED
    }

    private static final String PREFIX = "draw.grid.";

    /** Whether the grid is shown */
    public static final BooleanProperty ENABLED = new BooleanProperty(PREFIX + "enabled", false);
    /** The kind of grid */
    public static final EnumProperty<GridType> TYPE = new EnumProperty<>(PREFIX + "type", GridType.class, GridType.PROJECTED);
    /** Spacing of the vertical lines (longitude resp. east), in degrees resp. metres (true distance at the origin) */
    public static final DoubleProperty SPACING_X = new DoubleProperty(PREFIX + "spacing-x", 1000);
    /** Spacing of the horizontal lines (latitude resp. north), in degrees resp. metres (true distance at the origin) */
    public static final DoubleProperty SPACING_Y = new DoubleProperty(PREFIX + "spacing-y", 1000);
    /** Rotation of a projected grid, in degrees counter clockwise */
    public static final DoubleProperty ROTATION = new DoubleProperty(PREFIX + "rotation", 0);
    /** Origin of the grid: a grid line passes through this coordinate (longitude resp. east) */
    public static final DoubleProperty ORIGIN_X = new DoubleProperty(PREFIX + "origin-x", 0);
    /** Origin of the grid: a grid line passes through this coordinate (latitude resp. north) */
    public static final DoubleProperty ORIGIN_Y = new DoubleProperty(PREFIX + "origin-y", 0);
    /** Below this distance between lines (in pixels) the spacing is multiplied by 10 */
    public static final DoubleProperty MIN_PIXEL_SPACING = new DoubleProperty(PREFIX + "min-pixel-spacing", 25);
    /** The blend mode used to draw the lines */
    public static final EnumProperty<BlendComposite.Mode> BLEND_MODE
            = new EnumProperty<>(PREFIX + "blend-mode", BlendComposite.Mode.class, BlendComposite.Mode.HARD_LIGHT);
    /** Width of the lines in pixels */
    public static final DoubleProperty LINE_WIDTH = new DoubleProperty(PREFIX + "line-width", 2);
    /** Color of the lines of constant east coordinate resp. longitude (the lines running north-south) */
    public static final NamedColorProperty COLOR_EAST = new NamedColorProperty(marktr("grid east lines"), Color.YELLOW);
    /** Color of the lines of constant north coordinate resp. latitude (the lines running east-west) */
    public static final NamedColorProperty COLOR_NORTH = new NamedColorProperty(marktr("grid north lines"), Color.YELLOW);

    /**
     * A straight line of a projected grid.
     * @since 19640
     */
    public static final class ProjectedGridLine {
        /** start point */
        public final EastNorth start;
        /** end point */
        public final EastNorth end;
        /** {@code true} for a line of constant (rotated) east coordinate, {@code false} for constant north */
        public final boolean constantEast;

        ProjectedGridLine(EastNorth start, EastNorth end, boolean constantEast) {
            this.start = start;
            this.end = end;
            this.constantEast = constantEast;
        }

        @Override
        public String toString() {
            return (constantEast ? "east " : "north ") + start + " -> " + end;
        }
    }

    /**
     * A (curved) line of a latitude/longitude grid.
     * @since 19640
     */
    public static final class LatLonGridLine {
        /** the points of the polyline */
        public final List<LatLon> points;
        /** {@code true} for a meridian (constant longitude), {@code false} for a parallel (constant latitude) */
        public final boolean meridian;

        LatLonGridLine(List<LatLon> points, boolean meridian) {
            this.points = points;
            this.meridian = meridian;
        }

        @Override
        public String toString() {
            return (meridian ? "meridian " : "parallel ") + points;
        }
    }

    /** number of segments used to draw a curved (lat/lon) grid line across the view */
    private static final int CURVE_SEGMENTS = 32;
    /** hard limit for the number of lines in one direction, whatever the settings are */
    private static final int MAX_LINES = 500;

    /** written by {@link #paint} and read by {@link #preferenceChanged}, which may run on another thread */
    private volatile MapView mapView;

    /**
     * Constructs a new {@code MapGridPaintable}.
     */
    public MapGridPaintable() {
        Config.getPref().addPreferenceChangeListener(this);
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        mapView = mv;
        if (!Boolean.TRUE.equals(ENABLED.get()) || mv.getWidth() <= 0 || mv.getHeight() <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setStroke(new BasicStroke((float) Math.max(0.1, LINE_WIDTH.get())));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    Config.getPref().getBoolean("mappaint.use-antialiasing", true)
                    ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
            BlendComposite.Mode mode = BLEND_MODE.get();
            if (mode != null && mode != BlendComposite.Mode.NORMAL) {
                g2.setComposite(BlendComposite.getInstance(mode));
            }
            if (TYPE.get() == GridType.PROJECTED) {
                paintProjectedGrid(g2, mv);
            } else {
                paintLatLonGrid(g2, mv);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void paintProjectedGrid(Graphics2D g, MapView mv) {
        double sx = SPACING_X.get();
        double sy = SPACING_Y.get();
        if (sx <= 0 || sy <= 0) {
            return;
        }
        // the spacing is a true distance in metres, measured at the grid origin
        EastNorth origin = new EastNorth(ORIGIN_X.get(), ORIGIN_Y.get());
        double unitsPerMetre = projectionUnitsPerMetre(mv.getProjection(), origin);
        sx *= unitsPerMetre;
        sy *= unitsPerMetre;
        // thin out the grid until the cells are large enough on screen
        double factor = thinningFactor(Math.min(sx, sy) / mv.getScale());
        List<ProjectedGridLine> lines = getProjectedGridLines(mv.getProjectionBounds(), sx * factor, sy * factor,
                ROTATION.get(), origin.east(), origin.north());
        for (ProjectedGridLine line : lines) {
            g.setColor(line.constantEast ? COLOR_EAST.get() : COLOR_NORTH.get());
            g.draw(new Line2D.Double(mv.getPoint2D(line.start), mv.getPoint2D(line.end)));
        }
    }

    private static void paintLatLonGrid(Graphics2D g, MapView mv) {
        double sx = SPACING_X.get();
        double sy = SPACING_Y.get();
        if (sx <= 0 || sy <= 0) {
            return;
        }
        Bounds view = getVisibleLatLonBounds(mv);
        if (view == null) {
            return;
        }
        // pixel size of one cell at the center of the view
        LatLon center = view.getCenter();
        Point2D c = mv.getPoint2D(center);
        double factor = thinningFactor(Math.min(probePixels(mv, c, center, sx, true), probePixels(mv, c, center, sy, false)));
        List<LatLonGridLine> lines = getLatLonGridLines(view, mv.getProjection().getWorldBoundsLatLon(),
                sx * factor, sy * factor, ORIGIN_X.get(), ORIGIN_Y.get(), CURVE_SEGMENTS);
        for (LatLonGridLine line : lines) {
            g.setColor(line.meridian ? COLOR_EAST.get() : COLOR_NORTH.get());
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (LatLon ll : line.points) {
                Point2D p = mv.getPoint2D(ll);
                if (first) {
                    path.moveTo(p.getX(), p.getY());
                    first = false;
                } else {
                    path.lineTo(p.getX(), p.getY());
                }
            }
            g.draw(path);
        }
    }

    /**
     * Computes the latitude/longitude bounds of the part of the world which is visible in the given view.
     * <p>
     * Contrary to {@link org.openstreetmap.josm.gui.NavigatableComponent#getRealBounds()} this does not simply
     * convert the corners of the view: as soon as the view is larger than the world, those lie outside the world
     * and their longitude wraps around, which yields a range much narrower than what is really visible (and one
     * which jumps around while zooming or panning). The view is therefore first clipped to the world in
     * projected coordinates.
     * <p>
     * The clipped area is kept a hair inside the world, because a point exactly on the antimeridian converts to
     * an ambiguous longitude: {@link Projection#eastNorth2latlon} normalizes it to -180, which would turn the
     * visible range of a view showing e.g. 60° E to 180° into 180° W to 60° E, i.e. the other half of the world.
     * @param mv the map view
     * @return the visible bounds, or {@code null} if no part of the world is visible
     */
    static Bounds getVisibleLatLonBounds(MapView mv) {
        Projection projection = mv.getProjection();
        ProjectionBounds view = mv.getProjectionBounds();
        ProjectionBounds world = projection.getWorldBoundsBoxEastNorth();
        double minEast = Math.max(view.minEast, world.minEast);
        double maxEast = Math.min(view.maxEast, world.maxEast);
        double minNorth = Math.max(view.minNorth, world.minNorth);
        double maxNorth = Math.min(view.maxNorth, world.maxNorth);
        if (minEast >= maxEast || minNorth >= maxNorth) {
            return null;
        }
        // stay inside the world, but never collapse the area (the inset is a fraction of a millimetre on the ground)
        double insetEast = Math.min((maxEast - minEast) / 4, (world.maxEast - world.minEast) * 1e-9);
        double insetNorth = Math.min((maxNorth - minNorth) / 4, (world.maxNorth - world.minNorth) * 1e-9);
        minEast += insetEast;
        maxEast -= insetEast;
        minNorth += insetNorth;
        maxNorth -= insetNorth;
        Bounds bounds = null;
        for (double east : new double[] {minEast, maxEast}) {
            for (double north : new double[] {minNorth, maxNorth}) {
                LatLon ll = projection.eastNorth2latlon(new EastNorth(east, north));
                if (ll.isValid()) {
                    if (bounds == null) {
                        bounds = new Bounds(ll, false);
                    } else {
                        bounds.extend(ll);
                    }
                }
            }
        }
        return bounds != null ? bounds : mv.getRealBounds();
    }

    /**
     * Measures the distance on screen which corresponds to one grid spacing at the center of the view. The probe
     * is placed on whichever side of the center stays inside the valid coordinate range, and it is shortened if
     * the spacing itself does not fit, so that the result is a usable length at every zoom level.
     * @param mv the map view
     * @param center the center of the view, on screen
     * @param at the center of the view
     * @param spacing the grid spacing, in degrees
     * @param lon {@code true} to probe along the longitude, {@code false} along the latitude
     * @return the distance in pixels; 0 if it cannot be measured
     */
    private static double probePixels(MapView mv, Point2D center, LatLon at, double spacing, boolean lon) {
        double max = lon ? 180 : 89;
        double delta = Math.min(spacing, max);
        double value = lon ? at.lon() : at.lat();
        // probe towards the pole resp. the antimeridian, or backwards if that would leave the valid range
        double probe = value + delta <= max ? value + delta : value - delta;
        if (probe < -max || probe > max) {
            return 0;
        }
        Point2D p = mv.getPoint2D(lon ? new LatLon(at.lat(), probe) : new LatLon(probe, at.lon()));
        return center.distance(p) * spacing / delta;
    }

    /**
     * Computes how many projection units correspond to one metre on the ground at the given position. For
     * conformal projections (e.g. Mercator) this is the local scale factor, for a Mercator grid at 60° latitude
     * one metre is two projection units.
     * @param projection the projection
     * @param at the position (projected coordinates)
     * @return projection units per metre; 1 if it cannot be determined (position outside the world)
     */
    public static double projectionUnitsPerMetre(Projection projection, EastNorth at) {
        try {
            if (!projection.getWorldBoundsBoxEastNorth().contains(at)) {
                return 1;
            }
            double step = 100;
            LatLon a = projection.eastNorth2latlon(at);
            LatLon b = projection.eastNorth2latlon(new EastNorth(at.east() + step, at.north()));
            if (!a.isValid() || !b.isValid()) {
                return 1;
            }
            double metres = a.greatCircleDistance((ILatLon) b);
            return metres > 0 && Double.isFinite(metres) ? step / metres : 1;
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            return 1;
        }
    }

    /**
     * Computes the factor (a power of 10) by which the spacing must be multiplied so that the lines are at least
     * {@link #MIN_PIXEL_SPACING} apart.
     * @param pixelSpacing the distance between two lines on screen, in pixels
     * @return the factor (at least 1)
     */
    static double thinningFactor(double pixelSpacing) {
        double min = Math.max(1, MIN_PIXEL_SPACING.get());
        double factor = 1;
        if (pixelSpacing <= 0 || Double.isNaN(pixelSpacing)) {
            return factor;
        }
        while (pixelSpacing * factor < min && factor < 1e15) {
            factor *= 10;
        }
        return factor;
    }

    /**
     * Computes the lines of a (possibly rotated) grid in projected coordinates which cross the given area.
     * @param area the area to cover
     * @param spacingX distance between the lines running in the "north" direction of the grid (before rotation)
     * @param spacingY distance between the lines running in the "east" direction of the grid (before rotation)
     * @param rotationDegrees rotation of the grid, counter clockwise
     * @param originX east coordinate of the grid origin
     * @param originY north coordinate of the grid origin
     * @return the lines; empty if the spacing is invalid or there would be too many lines
     */
    public static List<ProjectedGridLine> getProjectedGridLines(ProjectionBounds area, double spacingX, double spacingY,
            double rotationDegrees, double originX, double originY) {
        List<ProjectedGridLine> lines = new ArrayList<>();
        if (!(spacingX > 0) || !(spacingY > 0)) {
            return lines;
        }
        double angle = Math.toRadians(rotationDegrees);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        // grid coordinates (u along the rotated east axis, v along the rotated north axis) of the area corners
        double[] xs = {area.minEast, area.maxEast, area.maxEast, area.minEast};
        double[] ys = {area.minNorth, area.minNorth, area.maxNorth, area.maxNorth};
        double uMin = Double.POSITIVE_INFINITY;
        double uMax = Double.NEGATIVE_INFINITY;
        double vMin = Double.POSITIVE_INFINITY;
        double vMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            double dx = xs[i] - originX;
            double dy = ys[i] - originY;
            double u = c * dx + s * dy;
            double v = -s * dx + c * dy;
            uMin = Math.min(uMin, u);
            uMax = Math.max(uMax, u);
            vMin = Math.min(vMin, v);
            vMax = Math.max(vMax, v);
        }
        long kuMin = (long) Math.ceil(uMin / spacingX);
        long kuMax = (long) Math.floor(uMax / spacingX);
        long kvMin = (long) Math.ceil(vMin / spacingY);
        long kvMax = (long) Math.floor(vMax / spacingY);
        if (kuMax - kuMin > MAX_LINES || kvMax - kvMin > MAX_LINES) {
            return lines;
        }
        // lines of constant u run along the v axis
        for (long k = kuMin; k <= kuMax; k++) {
            double u = k * spacingX;
            lines.add(new ProjectedGridLine(gridToEastNorth(u, vMin, c, s, originX, originY),
                    gridToEastNorth(u, vMax, c, s, originX, originY), true));
        }
        for (long k = kvMin; k <= kvMax; k++) {
            double v = k * spacingY;
            lines.add(new ProjectedGridLine(gridToEastNorth(uMin, v, c, s, originX, originY),
                    gridToEastNorth(uMax, v, c, s, originX, originY), false));
        }
        return lines;
    }

    private static EastNorth gridToEastNorth(double u, double v, double c, double s, double originX, double originY) {
        return new EastNorth(originX + c * u - s * v, originY + s * u + c * v);
    }

    /**
     * Computes the lines of a latitude/longitude grid which cross the given area. Since these lines are curves in
     * most projections, each line is returned as a polyline.
     * @param area the area to cover
     * @param world the bounds of the world in the current projection, the lines are clamped to it
     * @param spacingLon distance between the meridians, in degrees
     * @param spacingLat distance between the parallels, in degrees
     * @param originLon longitude of a meridian of the grid
     * @param originLat latitude of a parallel of the grid
     * @param segments number of segments of each polyline
     * @return the lines; empty if the spacing is invalid or there would be too many lines
     */
    public static List<LatLonGridLine> getLatLonGridLines(Bounds area, Bounds world, double spacingLon, double spacingLat,
            double originLon, double originLat, int segments) {
        List<LatLonGridLine> lines = new ArrayList<>();
        if (!(spacingLon > 0) || !(spacingLat > 0) || segments < 1) {
            return lines;
        }
        double minLat = Math.max(area.getMinLat(), world.getMinLat());
        double maxLat = Math.min(area.getMaxLat(), world.getMaxLat());
        double minLon = area.getMinLon();
        double maxLon = area.getMaxLon();
        if (maxLon < minLon) {
            maxLon += 360; // the view crosses the antimeridian
        }
        if (minLat >= maxLat || minLon >= maxLon) {
            return lines;
        }
        long kLonMin = (long) Math.ceil((minLon - originLon) / spacingLon);
        long kLonMax = (long) Math.floor((maxLon - originLon) / spacingLon);
        long kLatMin = (long) Math.ceil((minLat - originLat) / spacingLat);
        long kLatMax = (long) Math.floor((maxLat - originLat) / spacingLat);
        if (kLonMax - kLonMin > MAX_LINES || kLatMax - kLatMin > MAX_LINES) {
            return lines;
        }
        // for a projection which does not span the whole globe, meridians outside its longitude range are
        // skipped like the parallels below; a full range must not be checked since longitudes wrap around
        boolean limitedLon = world.getMinLon() > -180 || world.getMaxLon() < 180;
        for (long k = kLonMin; k <= kLonMax; k++) {
            double lon = LatLon.toIntervalLon(originLon + k * spacingLon);
            if (limitedLon && (lon < world.getMinLon() || lon > world.getMaxLon())) {
                continue;
            }
            List<LatLon> line = new ArrayList<>(segments + 1);
            for (int i = 0; i <= segments; i++) {
                line.add(new LatLon(minLat + (maxLat - minLat) * i / segments, lon));
            }
            lines.add(new LatLonGridLine(line, true));
        }
        for (long k = kLatMin; k <= kLatMax; k++) {
            double lat = originLat + k * spacingLat;
            if (lat < world.getMinLat() || lat > world.getMaxLat()) {
                continue;
            }
            addParallel(lines, lat, minLon, maxLon, segments);
        }
        return lines;
    }

    /**
     * Adds a parallel running from one longitude to another. A parallel which crosses the antimeridian is added
     * as two lines, one on each side of it: wrapping the longitudes of a single polyline would instead make it
     * jump right across the view.
     * @param lines the list to add to
     * @param lat the latitude of the parallel
     * @param minLon the longitude to start at, in [-180, 180]
     * @param maxLon the longitude to end at, may be larger than 180 if the area crosses the antimeridian
     * @param segments number of segments of each polyline
     */
    private static void addParallel(List<LatLonGridLine> lines, double lat, double minLon, double maxLon, int segments) {
        if (maxLon > 180) {
            addParallel(lines, lat, minLon, 180, segments);
            addParallel(lines, lat, -180, maxLon - 360, segments);
            return;
        }
        if (!(maxLon > minLon)) {
            return;
        }
        List<LatLon> line = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            line.add(new LatLon(lat, minLon + (maxLon - minLon) * i / segments));
        }
        lines.add(new LatLonGridLine(line, false));
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().startsWith(PREFIX) || e.getKey().equals(COLOR_EAST.getKey()) || e.getKey().equals(COLOR_NORTH.getKey())) {
            invalidate();
            if (mapView != null) {
                mapView.repaint();
            }
        }
    }

    @Override
    public void destroy() {
        Config.getPref().removePreferenceChangeListener(this);
        mapView = null;
    }
}
