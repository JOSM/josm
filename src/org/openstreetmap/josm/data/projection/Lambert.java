//License: GPL. For details, see LICENSE file.
//Thanks to Johan Montagnat and its geoconv java converter application
//(http://www.i3s.unice.fr/~johan/gps/ , published under GPL license)
//from which some code and constants have been reused here.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class Lambert implements Projection {
    /**
     * Lambert I, II, III, and IV projection exponents
     */
    public static final double n[] = { 0.7604059656, 0.7289686274, 0.6959127966, 0.6712679322 };

    /**
     * Lambert I, II, III, and IV projection constants
     */
    public static final double c[] = { 11603796.98, 11745793.39, 11947992.52, 12136281.99 };

    /**
     * Lambert I, II, III, and IV false east
     */
    public static final double Xs[] = { 600000.0, 600000.0, 600000.0, 234.358 };

    /**
     * Lambert I, II, III, and IV false north
     */
    public static final double Ys[] = { 5657616.674, 6199695.768, 6791905.085, 7239161.542 };

    /**
     * Lambert I, II, III, and IV longitudinal offset to Greenwich meridian
     */
    public static final double lg0 = 0.04079234433198; // 2deg20'14.025"

    /**
     * precision in iterative schema
     */

    public static final double epsilon = 1e-11;

    /**
     * France is divided in 4 Lambert projection zones (1,2,3 + 4th for Corsica)
     */
    public static final double cMaxLatZone1 = Math.toRadians(57 * 0.9);

    public static final double zoneLimits[] = { Math.toRadians(53.5 * 0.9), // between Zone 1 and Zone 2 (in grad *0.9)
        Math.toRadians(50.5 * 0.9), // between Zone 2 and Zone 3
        Math.toRadians(47.51963 * 0.9), // between Zone 3 and Zone 4
        Math.toRadians(46.17821 * 0.9) };// lowest latitude of Zone 4

    public static final double cMinLonZones = Math.toRadians(-4.9074074074074059 * 0.9);

    public static final double cMaxLonZones = Math.toRadians(10.2 * 0.9);

    /**
     *  Because josm cannot work correctly if two zones are displayed, we allow some overlapping
     */
    public static final double cMaxOverlappingZones = Math.toRadians(1.5 * 0.9);

    public static int layoutZone = -1;

    private static int currentZone = 0;

    /**
     * @param p  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Lambert Zone (ellipsoid Clark)
     */
    public EastNorth latlon2eastNorth(LatLon p) {
        // translate ellipsoid GRS80 (WGS83) => Clark
        LatLon geo = GRS802Clark(p);
        double lt = geo.lat(); // in radian
        double lg = geo.lon();

        // check if longitude and latitude are inside the French Lambert zones
        currentZone = 0;
        boolean outOfLambertZones = false;
        if (lt >= zoneLimits[3] && lt <= cMaxLatZone1 && lg >= cMinLonZones && lg <= cMaxLonZones) {
            // zone I
            if (lt > zoneLimits[0]) {
                currentZone = 0;
            } else if (lt > zoneLimits[1]) {
                currentZone = 1;
            } else if (lt > zoneLimits[2]) {
                currentZone = 2;
            } else if (lt > zoneLimits[3])
                // Note: zone IV is dedicated to Corsica island and extends from 47.8 to
                // 45.9 degrees of latitude. There is an overlap with zone III that can be
                // solved only with longitude (covers Corsica if lon > 7.2 degree)
                if (lg < Math.toRadians(8 * 0.9)) {
                    currentZone = 2;
                } else {
                    currentZone = 3;
                }
        } else {
            outOfLambertZones = true; // possible when MAX_LAT is used
        }
        if (!outOfLambertZones) {
            if (layoutZone == -1) {
                layoutZone = currentZone;
            } else if (layoutZone != currentZone) {
                if (farawayFromLambertZoneFrance(lt,lg)) {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("IMPORTANT : data positioned far away from\n"
                                    + "the current Lambert zone limits.\n"
                                    + "Do not upload any data after this message.\n"
                                    + "Undo your last action, save your work\n"
                                    + "and start a new layer on the new zone."),
                                    tr("Warning"),
                                    JOptionPane.WARNING_MESSAGE);
                    layoutZone = -1;
                } else {
                    System.out.println("temporarily extend Lambert zone " + layoutZone + " projection at lat,lon:"
                            + lt + "," + lg);
                }
            }
        }
        if (layoutZone == -1)
            return ConicProjection(lt, lg, Xs[currentZone], Ys[currentZone], c[currentZone], n[currentZone]);
        return ConicProjection(lt, lg, Xs[layoutZone], Ys[layoutZone], c[layoutZone], n[layoutZone]);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        LatLon geo;
        if (layoutZone == -1) {
            // possible until the Lambert zone is determined by latlon2eastNorth() with a valid LatLon
            geo = Geographic(p, Xs[currentZone], Ys[currentZone], c[currentZone], n[currentZone]);
        } else {
            geo = Geographic(p, Xs[layoutZone], Ys[layoutZone], c[layoutZone], n[layoutZone]);
        }
        // translate ellipsoid Clark => GRS80 (WGS83)
        LatLon wgs = Clark2GRS80(geo);
        return new LatLon(Math.toDegrees(wgs.lat()), Math.toDegrees(wgs.lon()));
    }

    @Override public String toString() {
        return tr("Lambert Zone (France)");
    }

    public String toCode() {
        return "EPSG:"+(27571+currentZone);
    }

    public String getCacheDirectoryName() {
        return "lambert";
    }

    /**
     * Initializes from geographic coordinates. Note that reference ellipsoid
     * used by Lambert is the Clark ellipsoid.
     *
     * @param lat latitude in grad
     * @param lon longitude in grad
     * @param Xs  false east (coordinate system origin) in meters
     * @param Ys  false north (coordinate system origin) in meters
     * @param c   projection constant
     * @param n   projection exponent
     * @return EastNorth projected coordinates in meter
     */
    private EastNorth ConicProjection(double lat, double lon, double Xs, double Ys, double c, double n) {
        double eslt = Ellipsoid.clarke.e * Math.sin(lat);
        double l = Math.log(Math.tan(Math.PI / 4.0 + (lat / 2.0))
                * Math.pow((1.0 - eslt) / (1.0 + eslt), Ellipsoid.clarke.e / 2.0));
        double east = Xs + c * Math.exp(-n * l) * Math.sin(n * (lon - lg0));
        double north = Ys - c * Math.exp(-n * l) * Math.cos(n * (lon - lg0));
        return new EastNorth(east, north);
    }

    /**
     * Initializes from projected coordinates (conic projection). Note that
     * reference ellipsoid used by Lambert is Clark
     *
     * @param eastNorth projected coordinates pair in meters
     * @param Xs        false east (coordinate system origin) in meters
     * @param Ys        false north (coordinate system origin) in meters
     * @param c         projection constant
     * @param n         projection exponent
     * @return LatLon in radian
     */
    private LatLon Geographic(EastNorth eastNorth, double Xs, double Ys, double c, double n) {
        double dx = eastNorth.east() - Xs;
        double dy = Ys - eastNorth.north();
        double R = Math.sqrt(dx * dx + dy * dy);
        double gamma = Math.atan(dx / dy);
        double l = -1.0 / n * Math.log(Math.abs(R / c));
        l = Math.exp(l);
        double lon = lg0 + gamma / n;
        double lat = 2.0 * Math.atan(l) - Math.PI / 2.0;
        double delta = 1.0;
        while (delta > epsilon) {
            double eslt = Ellipsoid.clarke.e * Math.sin(lat);
            double nlt = 2.0 * Math.atan(Math.pow((1.0 + eslt) / (1.0 - eslt), Ellipsoid.clarke.e / 2.0) * l) - Math.PI
            / 2.0;
            delta = Math.abs(nlt - lat);
            lat = nlt;
        }
        return new LatLon(lat, lon); // in radian
    }

    /**
     * Translate latitude/longitude in WGS84, (ellipsoid GRS80) to Lambert
     * geographic, (ellipsoid Clark)
     */
    private LatLon GRS802Clark(LatLon wgs) {
        double lat = Math.toRadians(wgs.lat()); // degree to radian
        double lon = Math.toRadians(wgs.lon());
        // WGS84 geographic => WGS84 cartesian
        double N = Ellipsoid.GRS80.a / (Math.sqrt(1.0 - Ellipsoid.GRS80.e2 * Math.sin(lat) * Math.sin(lat)));
        double X = (N/* +height */) * Math.cos(lat) * Math.cos(lon);
        double Y = (N/* +height */) * Math.cos(lat) * Math.sin(lon);
        double Z = (N * (1.0 - Ellipsoid.GRS80.e2)/* + height */) * Math.sin(lat);
        // WGS84 => Lambert ellipsoide similarity
        X += 168.0;
        Y += 60.0;
        Z += -320.0;
        // Lambert cartesian => Lambert geographic
        return Geographic(X, Y, Z, Ellipsoid.clarke);
    }

    private LatLon Clark2GRS80(LatLon lambert) {
        double lat = lambert.lat(); // in radian
        double lon = lambert.lon();
        // Lambert geographic => Lambert cartesian
        double N = Ellipsoid.clarke.a / (Math.sqrt(1.0 - Ellipsoid.clarke.e2 * Math.sin(lat) * Math.sin(lat)));
        double X = (N/* +height */) * Math.cos(lat) * Math.cos(lon);
        double Y = (N/* +height */) * Math.cos(lat) * Math.sin(lon);
        double Z = (N * (1.0 - Ellipsoid.clarke.e2)/* + height */) * Math.sin(lat);
        // Lambert => WGS84 ellipsoide similarity
        X += -168.0;
        Y += -60.0;
        Z += 320.0;
        // WGS84 cartesian => WGS84 geographic
        return Geographic(X, Y, Z, Ellipsoid.GRS80);
    }

    /**
     * initializes from cartesian coordinates
     *
     * @param X
     *            1st coordinate in meters
     * @param Y
     *            2nd coordinate in meters
     * @param Z
     *            3rd coordinate in meters
     * @param ell
     *            reference ellipsoid
     */
    private LatLon Geographic(double X, double Y, double Z, Ellipsoid ell) {
        double norm = Math.sqrt(X * X + Y * Y);
        double lg = 2.0 * Math.atan(Y / (X + norm));
        double lt = Math.atan(Z / (norm * (1.0 - (ell.a * ell.e2 / Math.sqrt(X * X + Y * Y + Z * Z)))));
        double delta = 1.0;
        while (delta > epsilon) {
            double s2 = Math.sin(lt);
            s2 *= s2;
            double l = Math.atan((Z / norm)
                    / (1.0 - (ell.a * ell.e2 * Math.cos(lt) / (norm * Math.sqrt(1.0 - ell.e2 * s2)))));
            delta = Math.abs(l - lt);
            lt = l;
        }
        double s2 = Math.sin(lt);
        s2 *= s2;
        // h = norm / Math.cos(lt) - ell.a / Math.sqrt(1.0 - ell.e2 * s2);
        return new LatLon(lt, lg);
    }

    private boolean farawayFromLambertZoneFrance(double lat, double lon) {
        if (lat < (zoneLimits[3] - cMaxOverlappingZones) || (lat > (cMaxLatZone1 + cMaxOverlappingZones))
                || (lon < (cMinLonZones - cMaxOverlappingZones)) || (lon > (cMaxLonZones + cMaxOverlappingZones)))
            return true;
        return false;
    }

    public Bounds getWorldBoundsLatLon()
    {
        // These are not the Lambert Zone boundaries but we keep these values until coordinates outside the
        // projection boundaries are handled correctly.
        return new Bounds(
                new LatLon(-85.05112877980659, -180.0),
                new LatLon(85.05112877980659, 180.0));
        /*return new Bounds(
                new LatLon(45.0, -4.9074074074074059),
                new LatLon(57.0, 10.2));*/
    }

    public double getDefaultZoomInPPD() {
        // TODO FIXME
        return 0;
    }
}
