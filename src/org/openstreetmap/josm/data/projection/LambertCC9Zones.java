//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * This class implements the Lambert Conic Conform 9 Zones projection as specified by the IGN
 * in this document http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
 * @author Pieren
 *
 */
public class LambertCC9Zones implements Projection {

    /**
     * Lambert 9 zones projection exponents
     */
    public static final double n[] = { 0.6691500006885269, 0.682018118346418, 0.6946784863203991, 0.7071272481559119,
        0.7193606118567315, 0.7313748510399917, 0.7431663060711892, 0.7547313851789208, 0.7660665655489937};

    /**
     * Lambert 9 zones projection constants
     */
    public static final double c[] = { 1.215363305807804E7, 1.2050261119223533E7, 1.195716926884592E7, 1.18737533925172E7,
        1.1799460698022118E7, 1.17337838820243E7, 1.16762559948139E7, 1.1626445901183508E7, 1.1583954251630554E7};

    /**
     * Lambert 9 zones false east
     */
    public static final double Xs = 1700000;

    /**
     * Lambert 9 zones false north
     */
    public static final double Ys[] = { 8293467.503439436, 9049604.665107645, 9814691.693461388, 1.0588107871787189E7,
        1.1369285637569271E7, 1.2157704903382052E7, 1.2952888086405803E7, 1.3754395745267643E7, 1.4561822739114787E7};

    /**
     * Lambert I, II, III, and IV longitudinal offset to Greenwich meridian
     */
    public static final double lg0 = 0.04079234433198; // 2deg20'14.025"

    /**
     * precision in iterative schema
     */

    public static final double epsilon = 1e-12;

    /**
     * France is divided in 9 Lambert projection zones, CC42 to CC50.
     */
    public static final double cMaxLatZones = Math.toRadians(51.1);

    public static final double cMinLatZones = Math.toRadians(41.0);

    public static final double cMinLonZones = Math.toRadians(-5.0);

    public static final double cMaxLonZones = Math.toRadians(10.2);

    public static final double lambda0 = Math.toRadians(3);
    public static final double e = Ellipsoid.GRS80.e; // but in doc=0.08181919112
    public static final double e2 =Ellipsoid.GRS80.e2;
    public static final double a = Ellipsoid.GRS80.a;
    /**
     *  Because josm cannot work correctly if two zones are displayed, we allow some overlapping
     */
    public static final double cMaxOverlappingZones = Math.toRadians(1.5);

    public static int layoutZone = -1;

    private double L(double phi, double e) {
        double sinphi = Math.sin(phi);
        return (0.5*Math.log((1+sinphi)/(1-sinphi))) - e/2*Math.log((1+e*sinphi)/(1-e*sinphi));
    }

    /**
     * @param p  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Lambert Zone (ellipsoid Clark)
     */
    public EastNorth latlon2eastNorth(LatLon p) {
        double lt = Math.toRadians(p.lat());
        double lg = Math.toRadians(p.lon());
        // check if longitude and latitude are inside the French Lambert zones and seek a zone number
        // if it is not already defined in layoutZone
        int possibleZone = 0;
        boolean outOfLambertZones = false;
        if (lt >= cMinLatZones && lt <= cMaxLatZones && lg >= cMinLonZones && lg <= cMaxLonZones) {
            /* with Lambert CC9 zones, each latitude is present in two zones. If the layout
               zone is not already set, we choose arbitrary the first possible zone */
            possibleZone = (int)p.lat()-42;
            if (possibleZone > 8) {
                possibleZone = 8;
            }
            if (possibleZone < 0) {
                possibleZone = 0;
            }
        } else {
            outOfLambertZones = true; // possible when MAX_LAT is used
        }
        if (!outOfLambertZones) {
            if (layoutZone == -1) {
                if (layoutZone != possibleZone) {
                    System.out.println("change Lambert zone from "+layoutZone+" to "+possibleZone);
                }
                layoutZone = possibleZone;
            } else if (Math.abs(layoutZone - possibleZone) > 1) {
                if (farawayFromLambertZoneFrance(lt, lg)) {
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
            return ConicProjection(lt, lg, possibleZone);
        return ConicProjection(lt, lg, layoutZone);
    }

    /**
     * 
     * @param lat latitude in grad
     * @param lon longitude in grad
     * @param nz Lambert CC zone number (from 1 to 9) - 1 !
     * @return EastNorth projected coordinates in meter
     */
    private EastNorth ConicProjection(double lat, double lon, int nz) {
        double R = c[nz]*Math.exp(-n[nz]*L(lat,e));
        double gamma = n[nz]*(lon-lambda0);
        double X = Xs + R*Math.sin(gamma);
        double Y = Ys[nz] + -R*Math.cos(gamma);
        return new EastNorth(X, Y);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        layoutZone  = north2ZoneNumber(p.north());
        return Geographic(p, layoutZone);
    }

    private LatLon Geographic(EastNorth ea, int nz) {
        double R = Math.sqrt(Math.pow(ea.getX()-Xs,2)+Math.pow(ea.getY()-Ys[nz], 2));
        double gamma = Math.atan((ea.getX()-Xs)/(Ys[nz]-ea.getY()));
        double lon = lambda0+gamma/n[nz];
        double latIso = (-1/n[nz])*Math.log(Math.abs(R/c[nz]));
        double lat = Ellipsoid.GRS80.latitude(latIso, e, epsilon);
        return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    @Override public String toString() {
        return tr("Lambert CC9 Zone (France)");
    }

    public static int north2ZoneNumber(double north) {
        int nz = (int)(north /1000000) - 1;
        if (nz < 0) return 0;
        else if (nz > 8) return 8;
        else return nz;
    }

    public static boolean isInL9CCZones(LatLon p) {
        double lt = Math.toRadians(p.lat());
        double lg = Math.toRadians(p.lon());
        if (lg >= cMinLonZones && lg <= cMaxLonZones && lt >= cMinLatZones && lt <= cMaxLatZones)
            return true;
        return false;
    }

    public String toCode() {
        if (layoutZone == -1)
            return "EPSG:"+(3942);
        return "EPSG:"+(3942+layoutZone); //CC42 is EPSG:3842 (up to EPSG:3950 for CC50)
    }

    public String getCacheDirectoryName() {
        return "lambert";
    }

    private boolean farawayFromLambertZoneFrance(double lat, double lon) {
        if (lat < (cMinLatZones - cMaxOverlappingZones) || (lat > (cMaxLatZones + cMaxOverlappingZones))
                || (lon < (cMinLonZones - cMaxOverlappingZones)) || (lon > (cMaxLonZones + cMaxOverlappingZones)))
            return true;
        return false;
    }

    public double getDefaultZoomInPPD() {
        // TODO Auto-generated method stub
        return 0;
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

}

