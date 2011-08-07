// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

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
 *
 * FIXME: nadgrids should probably be implemented as a Datum
 */
abstract public class AbstractProjection implements Projection {

    protected Ellipsoid ellps;
    protected Datum datum;
    protected Proj proj;
    protected double x_0 = 0.0;     /* false easting (in meters) */
    protected double y_0 = 0.0;     /* false northing (in meters) */
    protected double lon_0 = 0.0;   /* central meridian */
    protected double k_0 = 1.0;     /* general scale factor */
    protected NTV2GridShiftFile nadgrids = null;

    @Override
    public EastNorth latlon2eastNorth(LatLon ll) {
        if (nadgrids != null) {
            NTV2GridShift gs = new NTV2GridShift(ll);
            nadgrids.gridShiftReverse(gs);
            ll = new LatLon(ll.lat()+gs.getLatShiftDegrees(), ll.lon()+gs.getLonShiftPositiveEastDegrees());
        } else {
            ll = datum.fromWGS84(ll);
        }
        double[] en = proj.project(Math.toRadians(ll.lat()), Math.toRadians(ll.lon() - lon_0));
        return new EastNorth(ellps.a * k_0 * en[0] + x_0, ellps.a * k_0 * en[1] + y_0);
    }

    @Override
    public LatLon eastNorth2latlon(EastNorth en) {
        double[] latlon_rad = proj.invproject((en.east() - x_0) / ellps.a / k_0, (en.north() - y_0) / ellps.a / k_0);
        LatLon ll = new LatLon(Math.toDegrees(latlon_rad[0]), Math.toDegrees(latlon_rad[1]) + lon_0);
        if (nadgrids != null) {
            NTV2GridShift gs = new NTV2GridShift(ll);
            nadgrids.gridShiftForward(gs);
            ll = new LatLon(ll.lat()+gs.getLatShiftDegrees(), ll.lon()+gs.getLonShiftPositiveEastDegrees());
        } else {
            ll = datum.toWGS84(ll);
        }
        return ll;
    }

    @Override
    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m
        return 10;
    }
    
    /**
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
    
}
