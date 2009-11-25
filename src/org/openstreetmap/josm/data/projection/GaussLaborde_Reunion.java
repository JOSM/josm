// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class GaussLaborde_Reunion implements Projection {

    private static final double lon0 = Math.toRadians(55.53333333333333333333);
    private static final double lat0 = Math.toRadians(-21.11666666666666666666);
    private static final double x0 = 160000.0;
    private static final double y0 = 50000.0;
    private static final double k0 = 1;

    private static double sinLat0 = Math.sin(lat0);
    private static double cosLat0 = Math.cos(lat0);
    private static double n1 = Math.sqrt(1 + cosLat0*cosLat0*cosLat0*cosLat0*Ellipsoid.hayford.e2/(1-Ellipsoid.hayford.e2));
    private static double phic = Math.asin(sinLat0/n1);
    private static double c = Ellipsoid.hayford.latitudeIsometric(phic, 0) - n1*Ellipsoid.hayford.latitudeIsometric(lat0, Ellipsoid.hayford.e);
    private static double n2 = k0*Ellipsoid.hayford.a*Math.sqrt(1-Ellipsoid.hayford.e2)/(1-Ellipsoid.hayford.e2*sinLat0*sinLat0);
    private static double xs = x0;
    private static double ys = y0 - n2*phic;
    private static final double epsilon = 1e-11;
    private static final double scaleDiff = -32.3241E-6;
    private static final double Tx = 789.524;
    private static final double Ty = -626.486;
    private static final double Tz = -89.904;
    private static final double rx = Math.toRadians(0.6006/3600);
    private static final double ry = Math.toRadians(76.7946/3600);
    private static final double rz = Math.toRadians(-10.5788/3600);
    private static final double rx2 = rx*rx;
    private static final double ry2 = ry*ry;
    private static final double rz2 = rz*rz;

    public LatLon eastNorth2latlon(EastNorth p) {
        // plan Gauss-Laborde to geographic Piton-des-Neiges
        LatLon geo = Geographic(p);

        // geographic Piton-des-Neiges/Hayford to geographic WGS84/GRS80
        LatLon wgs = PTN2GRS80(geo);
        return new LatLon(Math.toDegrees(wgs.lat()), Math.toDegrees(wgs.lon()));
    }

    /*
     * Convert projected coordinates (Gauss-Laborde) to reference ellipsoid Hayforf geographic Piton-des-Neiges
     * @return geographic coordinates in radian
     */
    private LatLon Geographic(EastNorth eastNorth) {
        double dxn = (eastNorth.east()-xs)/n2;
        double dyn = (eastNorth.north()-ys)/n2;
        double lambda = Math.atan(sinh(dxn)/Math.cos(dyn));
        double latIso = Ellipsoid.hayford.latitudeIsometric(Math.asin(Math.sin(dyn)/cosh(dxn)), 0);
        double lon = lon0 + lambda/n1;
        double lat = Ellipsoid.hayford.latitude((latIso-c)/n1, Ellipsoid.hayford.e, 1E-12);
        return new LatLon(lat, lon);
    }

    /**
     * Convert geographic Piton-des-Neiges ellipsoid Hayford to geographic WGS84/GRS80
     * @param PTN in radian
     * @return
     */
    private LatLon PTN2GRS80(LatLon PTN) {
        double lat = PTN.lat();
        double lon = PTN.lon();
        double N = Ellipsoid.hayford.a / (Math.sqrt(1.0 - Ellipsoid.hayford.e2 * Math.sin(lat) * Math.sin(lat)));
        double X = (N/* +height */) * Math.cos(lat) * Math.cos(lon);
        double Y = (N/* +height */) * Math.cos(lat) * Math.sin(lon);
        double Z = (N * (1.0 - Ellipsoid.hayford.e2)/* + height */) * Math.sin(lat);

        // translation
        double coord[] = sevenParametersTransformation(X, Y, Z);

        // WGS84 cartesian => WGS84 geographic
        return cart2LatLon(coord[0], coord[1], coord[2], Ellipsoid.GRS80);
    }

    /**
     * 7 parameters transformation
     * @param coord X, Y, Z in array
     * @return transformed X, Y, Z in array
     */
    private double[] sevenParametersTransformation(double Xa, double Ya, double Za){
        double Xb = Tx + Xa*(1+scaleDiff) + Za*ry - Ya*rz;
        double Yb = Ty + Ya*(1+scaleDiff) + Xa*rz - Za*rx;
        double Zb = Tz + Za*(1+scaleDiff) + Ya*rx - Xa*ry;
        return new double[]{Xb, Yb, Zb};
    }

    public EastNorth latlon2eastNorth(LatLon wgs) {
        // translate ellipsoid GRS80 (WGS83) => reference ellipsoid geographic R\u00E9union
        LatLon geo = GRS802Hayford(wgs);

        // reference ellipsoid geographic => GaussLaborde plan
        return GaussLabordeProjection(geo);
    }

    private LatLon GRS802Hayford(LatLon wgs) {
        double lat = Math.toRadians(wgs.lat()); // degree to radian
        double lon = Math.toRadians(wgs.lon());
        // WGS84 geographic => WGS84 cartesian
        double N = Ellipsoid.GRS80.a / (Math.sqrt(1.0 - Ellipsoid.GRS80.e2 * Math.sin(lat) * Math.sin(lat)));
        double X = (N/* +height */) * Math.cos(lat) * Math.cos(lon);
        double Y = (N/* +height */) * Math.cos(lat) * Math.sin(lon);
        double Z = (N * (1.0 - Ellipsoid.GRS80.e2)/* + height */) * Math.sin(lat);
        // translation
        double coord[] = invSevenParametersTransformation(X, Y, Z);
        // Gauss cartesian => Gauss geographic
        return Geographic(coord[0], coord[1], coord[2], Ellipsoid.hayford);
    }

    /**
     * 7 parameters inverse transformation
     * @param coord X, Y, Z in array
     * @return transformed X, Y, Z in array
     */
    private double[] invSevenParametersTransformation(double Vx, double Vy, double Vz){
        Vx = Vx - Tx;
        Vy = Vy - Ty;
        Vz = Vz - Tz;
        double e = 1 + scaleDiff;
        double e2 = e*e;
        double det = e*(e2 + rx2 + ry2 + rz2);
        double Ux = ((e2 + rx2)*Vx + (e*rz + rx*ry)*Vy + (rx*rz - e*ry)*Vz)/det;
        double Uy = ((-e*rz + rx*ry)*Vx + (e2 + ry2)*Vy + (e*rx + ry*rz)*Vz)/det;
        double Uz = ((e*ry + rx*rz)*Vx + (-e*rx + ry*rz)*Vy + (e2 + rz2)*Vz)/det;
        return new double[]{Ux, Uy, Uz};
    }

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

    private EastNorth GaussLabordeProjection(LatLon geo) {
        double lambda = n1*(geo.lon()-lon0);
        double latIso = c + n1*Ellipsoid.hayford.latitudeIsometric(geo.lat());
        double x = xs + n2*Ellipsoid.hayford.latitudeIsometric(Math.asin(Math.sin(lambda)/((Math.exp(latIso) + Math.exp(-latIso))/2)),0);
        double y = ys + n2*Math.atan(((Math.exp(latIso) - Math.exp(-latIso))/2)/(Math.cos(lambda)));
        return new EastNorth(x, y);
    }

    /**
     * initializes from cartesian coordinates
     *
     * @param X 1st coordinate in meters
     * @param Y 2nd coordinate in meters
     * @param Z 3rd coordinate in meters
     * @param ell reference ellipsoid
     */
    private LatLon cart2LatLon(double X, double Y, double Z, Ellipsoid ell) {
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

    /*
     * hyperbolic sine
     */
    public static final double sinh(double x) {
        return (Math.exp(x) - Math.exp(-x))/2;
    }
    /*
     * hyperbolic cosine
     */
    public static final double cosh(double x) {
        return (Math.exp(x) + Math.exp(-x))/2;
    }

    public String getCacheDirectoryName() {
        return this.toString();
    }

    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(-21.5, 55.14),
                new LatLon(-20.76, 55.94));
    }

    public String toCode() {
        return "EPSG::3727";
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override public String toString() {
        return tr("Gauss-Laborde R\u00E9union 1947");
    }

    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m (in default scale, 1 pixel will be 10 meters)
        return 10.0;
    }
}
