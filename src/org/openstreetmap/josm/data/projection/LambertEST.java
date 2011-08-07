// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;

/**
 * Estonian Coordinate System of 1997.
 * 
 * Thanks to Johan Montagnat and its geoconv java converter application
 * (http://www.i3s.unice.fr/~johan/gps/ , published under GPL license)
 * from which some code and constants have been reused here.
 */
public class LambertEST extends AbstractProjection {

    public LambertEST() {
        ellps = Ellipsoid.GRS80;
        datum = GRS80Datum.INSTANCE;
        lon_0 = 24;
        double lat_0 = 57.517553930555555555555555555556;
        double lat_1 = 59 + 1.0/3.0;
        double lat_2 = 58;
        x_0 = 500000;
        y_0 = 6375000;
        proj = new LambertConformalConic();
        ((LambertConformalConic) proj).updateParameters2SP(ellps, lat_0, lat_1, lat_2);
    }

    @Override
    public String toString() {
        return tr("Lambert Zone (Estonia)");
    }

    @Override
    public Integer getEpsgCode() {
        return 3301;
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override
    public String getCacheDirectoryName() {
        return "lambertest";
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(56.05, 21.64),
                new LatLon(61.13, 28.58));
    }

}
