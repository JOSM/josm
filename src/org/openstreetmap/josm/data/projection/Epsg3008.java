// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * SWEREF99 13 30 projection. Based on data from spatialreference.org.
 * http://spatialreference.org/ref/epsg/3008/
 *
 * @author Hanno Hecker, based on the TransverseMercatorLV.java by Viesturs Zarins
 */
public class Epsg3008 extends TransverseMercator {

    private final static double UTMScaleFactor = 1.0;
    private double UTMCentralMeridianRad;
    private double offsetEastMeters = 150000;
    private double offsetNorthMeters = 0;

    public Epsg3008()
    {
	UTMCentralMeridianRad = Math.toRadians(13.5);
    }

    @Override public String toString() {
        return tr("SWEREF99 13 30 / EPSG:3008 (Sweden)");
    }

    private int epsgCode() {
        return 3008;
    }

    @Override
    public String toCode() {
        return "EPSG:"+ epsgCode();
    }

    @Override
    public int hashCode() {
        return toCode().hashCode();
    }

    public String getCacheDirectoryName() {
        return "epsg"+ epsgCode();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(55.2, 12.1),     // new LatLon(-90.0, -180.0),
                new LatLon(62.26, 14.65));  // new LatLon(90.0, 180.0));
    }

    @Override
    public EastNorth latlon2eastNorth(LatLon p) {
        EastNorth a = mapLatLonToXY(Math.toRadians(p.lat()), Math.toRadians(p.lon()), UTMCentralMeridianRad);
        return new EastNorth(a.east() * UTMScaleFactor + offsetEastMeters, a.north() * UTMScaleFactor + offsetNorthMeters);
    }

    @Override
    public LatLon eastNorth2latlon(EastNorth p) {
        return mapXYToLatLon((p.east() - offsetEastMeters)/UTMScaleFactor, (p.north() - offsetNorthMeters)/UTMScaleFactor, UTMCentralMeridianRad);
    }

}
