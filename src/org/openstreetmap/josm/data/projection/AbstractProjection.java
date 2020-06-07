// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.tools.Utils;

/**
 * Implementation of the Projection interface that represents a coordinate reference system and delegates
 * the real projection and datum conversion to other classes.
 *
 * It handles false easting and northing, central meridian and general scale factor before calling the
 * delegate projection.
 *
 * Forwards lat/lon values to the real projection in units of radians.
 *
 * The fields are named after Proj.4 parameters.
 *
 * Subclasses of AbstractProjection must set ellps and proj to a non-null value.
 * In addition, either datum or nadgrid has to be initialized to some value.
 */
public abstract class AbstractProjection implements Projection {

    protected Ellipsoid ellps;
    protected Datum datum;
    protected Proj proj;
    protected double x0;            /* false easting (in meters) */
    protected double y0;            /* false northing (in meters) */
    protected double lon0;          /* central meridian */
    protected double pm;            /* prime meridian */
    protected double k0 = 1.0;      /* general scale factor */
    protected double toMeter = 1.0; /* switch from meters to east/north coordinate units */

    private volatile ProjectionBounds projectionBoundsBox;

    /**
     * Get the base ellipsoid that this projection uses.
     * @return The {@link Ellipsoid}
     */
    public final Ellipsoid getEllipsoid() {
        return ellps;
    }

    /**
     * Gets the datum this projection is based on.
     * @return The datum
     */
    public final Datum getDatum() {
        return datum;
    }

    /**
     * Replies the projection (in the narrow sense)
     * @return The projection object
     */
    public final Proj getProj() {
        return proj;
    }

    /**
     * Gets an east offset that gets applied when converting the coordinate
     * @return The offset to apply in meter
     */
    public final double getFalseEasting() {
        return x0;
    }

    /**
     * Gets an north offset that gets applied when converting the coordinate
     * @return The offset to apply in meter
     */
    public final double getFalseNorthing() {
        return y0;
    }

    /**
     * Gets the meridian that this projection is centered on.
     * @return The longitude of the meridian.
     */
    public final double getCentralMeridian() {
        return lon0;
    }

    public final double getScaleFactor() {
        return k0;
    }

    /**
     * Get the factor that converts meters to intended units of east/north coordinates.
     *
     * For projected coordinate systems, the semi-major axis of the ellipsoid is
     * always given in meters, which means the preliminary projection result will
     * be in meters as well. This factor is used to convert to the intended units
     * of east/north coordinates (e.g. feet in the US).
     *
     * For geographic coordinate systems, the preliminary "projection" result will
     * be in degrees, so there is no reason to convert anything and this factor
     * will by 1 by default.
     *
     * @return factor that converts meters to intended units of east/north coordinates
     */
    public final double getToMeter() {
        return toMeter;
    }

    @Override
    public EastNorth latlon2eastNorth(ILatLon toConvert) {
        // TODO: Use ILatLon in datum, so we don't need to wrap it here.
        LatLon ll = datum.fromWGS84(new LatLon(toConvert));
        double[] en = proj.project(Utils.toRadians(ll.lat()), Utils.toRadians(LatLon.normalizeLon(ll.lon() - lon0 - pm)));
        return new EastNorth(
                (ellps.a * k0 * en[0] + x0) / toMeter,
                (ellps.a * k0 * en[1] + y0) / toMeter);
    }

    @Override
    public LatLon eastNorth2latlon(EastNorth en) {
        // We know it is a latlon. Nice would be to change this method return type to ILatLon
        return eastNorth2latlon(en, LatLon::normalizeLon);
    }

    @Override
    public LatLon eastNorth2latlonClamped(EastNorth en) {
        ILatLon ll = eastNorth2latlon(en, lon -> Utils.clamp(lon, -180, 180));
        Bounds bounds = getWorldBoundsLatLon();
        return new LatLon(Utils.clamp(ll.lat(), bounds.getMinLat(), bounds.getMaxLat()),
                          Utils.clamp(ll.lon(), bounds.getMinLon(), bounds.getMaxLon()));
    }

    private LatLon eastNorth2latlon(EastNorth en, DoubleUnaryOperator normalizeLon) {
        double[] latlonRad = proj.invproject(
                 (en.east() * toMeter - x0) / ellps.a / k0,
                (en.north() * toMeter - y0) / ellps.a / k0);
        double lon = Utils.toDegrees(latlonRad[1]) + lon0 + pm;
        LatLon ll = new LatLon(Utils.toDegrees(latlonRad[0]), normalizeLon.applyAsDouble(lon));
        return datum.toWGS84(ll);
    }

    @Override
    public Map<ProjectionBounds, Projecting> getProjectingsForArea(ProjectionBounds area) {
        if (proj.lonIsLinearToEast()) {
            //FIXME: Respect datum?
            // wrap the world around
            Bounds bounds = getWorldBoundsLatLon();
            double minEast = latlon2eastNorth(bounds.getMin()).east();
            double maxEast = latlon2eastNorth(bounds.getMax()).east();
            double dEast = maxEast - minEast;
            if ((area.minEast < minEast || area.maxEast > maxEast) && dEast > 0) {
                // We could handle the dEast < 0 case but we don't need it atm.
                int minChunk = (int) Math.floor((area.minEast - minEast) / dEast);
                int maxChunk = (int) Math.floor((area.maxEast - minEast) / dEast);
                HashMap<ProjectionBounds, Projecting> ret = new HashMap<>();
                for (int chunk = minChunk; chunk <= maxChunk; chunk++) {
                    ret.put(new ProjectionBounds(Math.max(area.minEast, minEast + chunk * dEast), area.minNorth,
                                                 Math.min(area.maxEast, maxEast + chunk * dEast), area.maxNorth),
                            new ShiftedProjecting(this, new EastNorth(-chunk * dEast, 0)));
                }
                return ret;
            }
        }

        return Collections.singletonMap(area, this);
    }

    @Override
    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m
        return 10 / getMetersPerUnit();
    }

    /**
     * Returns The EPSG Code of this CRS.
     * @return The EPSG Code of this CRS, null if it doesn't have one.
     */
    public abstract Integer getEpsgCode();

    /**
     * Default implementation of toCode().
     * Should be overridden, if there is no EPSG code for this CRS.
     */
    @Override
    public String toCode() {
        return "EPSG:" + getEpsgCode();
    }

    protected static final double convertMinuteSecond(double minute, double second) {
        return (minute/60.0) + (second/3600.0);
    }

    protected static final double convertDegreeMinuteSecond(double degree, double minute, double second) {
        return degree + (minute/60.0) + (second/3600.0);
    }

    @Override
    public final ProjectionBounds getWorldBoundsBoxEastNorth() {
        ProjectionBounds result = projectionBoundsBox;
        if (result == null) {
            synchronized (this) {
                result = projectionBoundsBox;
                if (result == null) {
                    ProjectionBounds bds = new ProjectionBounds();
                    visitOutline(getWorldBoundsLatLon(), 1000, bds::extend);
                    projectionBoundsBox = bds;
                }
            }
        }
        return projectionBoundsBox;
    }

    @Override
    public Projection getBaseProjection() {
        return this;
    }

    @Override
    public void visitOutline(Bounds b, Consumer<EastNorth> visitor) {
        visitOutline(b, 100, visitor);
    }

    private void visitOutline(Bounds b, int nPoints, Consumer<EastNorth> visitor) {
        double maxlon = b.getMaxLon();
        if (b.crosses180thMeridian()) {
            maxlon += 360.0;
        }
        double spanLon = maxlon - b.getMinLon();
        double spanLat = b.getMaxLat() - b.getMinLat();

        //TODO: Use projection to see if there is any need for doing this along each axis.
        for (int step = 0; step < nPoints; step++) {
            visitor.accept(latlon2eastNorth(
                    new LatLon(b.getMinLat(), b.getMinLon() + spanLon * step / nPoints)));
        }
        for (int step = 0; step < nPoints; step++) {
            visitor.accept(latlon2eastNorth(
                    new LatLon(b.getMinLat() + spanLat * step / nPoints, maxlon)));
        }
        for (int step = 0; step < nPoints; step++) {
            visitor.accept(latlon2eastNorth(
                    new LatLon(b.getMaxLat(), maxlon - spanLon * step / nPoints)));
        }
        for (int step = 0; step < nPoints; step++) {
            visitor.accept(latlon2eastNorth(
                    new LatLon(b.getMaxLat() - spanLat * step / nPoints, b.getMinLon())));
        }
    }
}
