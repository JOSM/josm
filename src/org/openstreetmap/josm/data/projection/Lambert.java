//License: GPL. For details, see LICENSE file.
//Thanks to Johan Montagnat and its geoconv java converter application
//(http://www.i3s.unice.fr/~johan/gps/ , published under GPL license) 
//from which some code and constants have been reused here.
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class Lambert implements Projection {
    /**
     * Lambert I, II, III, and IV projection exponents
     */
    public static final double n[] = {
        0.7604059656, 0.7289686274, 0.6959127966, 0.6712679322
    };
    /**
     * Lambert I, II, III, and IV projection constants
     */
    public static final double c[] = {
        11603796.98, 11745793.39, 11947992.52, 12136281.99
    };
    /**
     * Lambert I, II, III, and IV false east
     */
    public static final double Xs[] = {
        600000.0, 600000.0, 600000.0, 234.358
    };
    /**
     * Lambert I, II, III, and IV false north
     */
    public static final double Ys[] = {
        5657616.674, 6199695.768, 6791905.085, 7239161.542
    };
    /**
     * Lambert I, II, III, and IV longitudinal offset to Greenwich meridian
     */
    public static final double lg0 = 0.04079234433198; // 2 deg 20 min 14.025 sec
    /**
     * precision in iterative schema
     */
    public static final double epsilon = 1e-11;

    public static int zone = 0;

    public EastNorth latlon2eastNorth(LatLon p) {
        // check if longitude and latitude are inside the french Lambert zones
        double lt = p.lat();
        double lg = p.lon(); 
        if(lt > 57*9/10 || lt < 46.17821*9/10 ||
                lg > 10.2*9/10 || lg < -4.9074074074074059*9/10) {
            if (lt > 57*9/10) {
                // out of Lambert zones, possible when MAX_LAT is used- keep current zone
            }
            // zone I
            else if (lt > 53.5*9/10)
                zone = 0;
            // zone II
            else if(lt > 50.5*9/10)
                zone = 1;
            // zone III
            else if(lt > 47.51963*9/10)
                zone = 2;
            else if (lt > 46.17821*9/10)
                // zone III or IV
                // Note: zone IV is dedicated to Corsica island and extends 
                // from 47.8 to 45.9 degrees of latitude. There is an overlap with 
                // zone III that can  be solved only with longitude 
                // (covers Corsica if lon > 7.2 degree)
                if (lg < 8*9/10)
                    zone = 2;
                else
                    zone = 3;
        } else {
            // out of Lambert zones- keep current one
        }
        EastNorth eastNorth = ConicProjection(lt, lg, Xs[zone], Ys[zone], c[zone], n[zone]);
        return eastNorth;
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        return Geographic(p, Xs[zone], Ys[zone], c[zone], n[zone]);
    }

    @Override public String toString() {
        return "Lambert";
    }

    public String getCacheDirectoryName() {
        return "lambert";
    }

    public double scaleFactor() {
        return 1.0/360;
    }

    @Override public boolean equals(Object o) {
        return o instanceof Lambert;
    }

    @Override public int hashCode() {
        return Lambert.class.hashCode();
    }

    /**
     * Initializes from geographic coordinates.
     * Note that reference ellipsoid used by Lambert is the Clark ellipsoid. 
     *
     * @param lat latitude in degree
     * @param lon longitude in degree
     * @param Xs false east (coordinate system origin) in meters
     * @param Ys false north (coordinate system origin) in meters
     * @param c projection constant
     * @param n projection exponent
     * @return EastNorth projected coordinates in meter 
     */
    public EastNorth ConicProjection(double lat, double lon, double Xs, double Ys,
            double c, double n) {
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);
        double eslt = Ellipsoid.clarke.e * Math.sin(lat);
        double l = Math.log(Math.tan(Math.PI/4.0 + (lat/2.0)) *
                Math.pow((1.0 - eslt)/(1.0 + eslt), Ellipsoid.clarke.e/2.0));
        double east = Xs + c * Math.exp(-n * l) * Math.sin(n * (lon - lg0));
        double north = Ys - c * Math.exp(-n * l) * Math.cos(n * (lon - lg0));
        return new EastNorth(east, north);
    }
    /**
     * Initializes from projected coordinates (conic projection).
     * Note that reference ellipsoid used by Lambert is Clark 
     *
     * @param coord projected coordinates pair in meters
     * @param Xs false east (coordinate system origin) in meters
     * @param Ys false north (coordinate system origin) in meters
     * @param c projection constant
     * @param n projection exponent
     * @return LatLon in degrees
     */
    public LatLon Geographic(EastNorth eastNorth, double Xs, double Ys,
            double c, double n) {
        double dx = eastNorth.east() - Xs;
        double dy = Ys - eastNorth.north();
        double R = Math.sqrt(dx*dx + dy*dy);
        double gamma = Math.atan(dx / dy);
        double l = -1.0/n * Math.log(Math.abs(R / c));
        l = Math.exp(l);
        double lon = lg0 + gamma / n;
        double lat = 2.0 * Math.atan(l) - Math.PI/2.0;
        double delta = 1.0;
        while(delta > epsilon) {
            double eslt = Ellipsoid.clarke.e * Math.sin(lat);
            double nlt = 2.0 * Math.atan(Math.pow((1.0 + eslt)/(1.0 - eslt), Ellipsoid.clarke.e/2.0)
                    * l) - Math.PI/2.0;
            delta = Math.abs(nlt - lat);
            lat = nlt;
        }
        return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon));
    }
}
