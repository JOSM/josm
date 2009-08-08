// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

/**
 * This class is not a direct implementation of Projection. It collects all methods used
 * for the projection of the French departements in the Caribbean Sea UTM zone 20N
 * but using each time different local geodesic settings for the 7 parameters transformation algorithm.
 */
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class UTM_20N_France_DOM {

    /**
     * false east in meters (constant)
     */
    private static final double Xs = 500000.0;
    /**
     * false north in meters (0 in northern hemisphere, 10000000 in southern
     * hemisphere)
     */
    private static double Ys = 0;
    /**
     * origin meridian longitude
     */
    protected double lg0;
    /**
     * UTM zone (from 1 to 60)
     */
    int zone = 20;
    /**
     * whether north or south hemisphere
     */
    private boolean isNorth = true;
    /**
     * 7 parameters transformation
     */
    double tx = 0.0;
    double ty = 0.0;
    double tz = 0.0;
    double rx = 0;
    double ry = 0;
    double rz = 0;
    double scaleDiff = 0;
    /**
     * precision in iterative schema
     */
    public static final double epsilon = 1e-11;
    public final static double DEG_TO_RAD = Math.PI / 180;
    public final static double RAD_TO_DEG = 180 / Math.PI;

    public UTM_20N_France_DOM(double[] translation, double[] rotation, double scaleDiff) {
        this.tx = translation[0];
        this.ty = translation[1];
        this.tz = translation[2];
        this.rx = rotation[0]/206264.806247096355; // seconds to radian
        this.ry = rotation[1]/206264.806247096355;
        this.rz = rotation[2]/206264.806247096355;
        this.scaleDiff = scaleDiff;
    }
    public EastNorth latlon2eastNorth(LatLon p) {
        // translate ellipsoid GRS80 (WGS83) => reference ellipsoid geographic
        LatLon geo = GRS802Hayford(p);

        // reference ellipsoid geographic => UTM projection
        return MTProjection(geo, Ellipsoid.hayford.a, Ellipsoid.hayford.e);
    }

    /**
     * Translate latitude/longitude in WGS84, (ellipsoid GRS80) to UTM
     * geographic, (ellipsoid Hayford)
     */
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
        // UTM cartesian => UTM geographic
        return Geographic(coord[0], coord[1], coord[2], Ellipsoid.hayford);
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

    /**
     * initalizes from geographic coordinates
     *
     * @param coord geographic coordinates triplet
     * @param a reference ellipsoid long axis
     * @param e reference ellipsoid excentricity
     */
    private EastNorth MTProjection(LatLon coord, double a, double e) {
        double n = 0.9996 * a;
        Ys = (coord.lat() >= 0.0) ? 0.0 : 10000000.0;
        double r6d = Math.PI / 30.0;
        //zone = (int) Math.floor((coord.lon() + Math.PI) / r6d) + 1;
        lg0 = r6d * (zone - 0.5) - Math.PI;
        double e2 = e * e;
        double e4 = e2 * e2;
        double e6 = e4 * e2;
        double e8 = e4 * e4;
        double C[] = {
                1.0 - e2/4.0 - 3.0*e4/64.0 - 5.0*e6/256.0 - 175.0*e8/16384.0,
                e2/8.0 - e4/96.0 - 9.0*e6/1024.0 - 901.0*e8/184320.0,
                13.0*e4/768.0 + 17.0*e6/5120.0 - 311.0*e8/737280.0,
                61.0*e6/15360.0 + 899.0*e8/430080.0,
                49561.0*e8/41287680.0
        };
        double s = e * Math.sin(coord.lat());
        double l = Math.log(Math.tan(Math.PI/4.0 + coord.lat()/2.0) *
                Math.pow((1.0 - s) / (1.0 + s), e/2.0));
        double phi = Math.asin(Math.sin(coord.lon() - lg0) /
                ((Math.exp(l) + Math.exp(-l)) / 2.0));
        double ls = Math.log(Math.tan(Math.PI/4.0 + phi/2.0));
        double lambda = Math.atan(((Math.exp(l) - Math.exp(-l)) / 2.0) /
                Math.cos(coord.lon() - lg0));

        double north = C[0] * lambda;
        double east = C[0] * ls;
        for(int k = 1; k < 5; k++) {
            double r = 2.0 * k * lambda;
            double m = 2.0 * k * ls;
            double em = Math.exp(m);
            double en = Math.exp(-m);
            double sr = Math.sin(r)/2.0 * (em + en);
            double sm = Math.cos(r)/2.0 * (em - en);
            north += C[k] * sr;
            east += C[k] * sm;
        }
        east *= n;
        east += Xs;
        north *= n;
        north += Ys;
        return new EastNorth(east, north);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        MTProjection(p.east(), p.north(), zone, isNorth);
        LatLon geo = Geographic(p, Ellipsoid.hayford.a, Ellipsoid.hayford.e, 0.0 /* z */);

        // reference ellipsoid geographic => reference ellipsoid cartesian
        //Hayford2GRS80(ellipsoid, geo);
        double N = Ellipsoid.hayford.a / (Math.sqrt(1.0 - Ellipsoid.hayford.e2 * Math.sin(geo.lat()) * Math.sin(geo.lat())));
        double X = (N /*+ h*/) * Math.cos(geo.lat()) * Math.cos(geo.lon());
        double Y = (N /*+ h*/) * Math.cos(geo.lat()) * Math.sin(geo.lon());
        double Z = (N * (1.0-Ellipsoid.hayford.e2) /*+ h*/) * Math.sin(geo.lat());

        // translation
        double coord[] = sevenParametersTransformation(X, Y, Z);

        // WGS84 cartesian => WGS84 geographic
        LatLon wgs = cart2LatLon(coord[0], coord[1], coord[2], Ellipsoid.GRS80);
        return new LatLon(Math.toDegrees(wgs.lat()), Math.toDegrees(wgs.lon()));
    }

    /**
     * initializes new projection coordinates (in north hemisphere)
     *
     * @param east east from origin in meters
     * @param north north from origin in meters
     * @param zone zone number (from 1 to 60)
     * @param isNorth true in north hemisphere, false in south hemisphere
     */
    private void MTProjection(double east, double north, int zone, boolean isNorth) {
        Ys = isNorth ? 0.0 : 10000000.0;
        double r6d = Math.PI / 30.0;
        lg0 = r6d * (zone - 0.5) - Math.PI;
    }

    public double scaleFactor() {
        return 1/Math.PI/2;
    }

    /**
     * initalizes from projected coordinates (Mercator transverse projection)
     *
     * @param coord projected coordinates pair
     * @param e reference ellipsoid excentricity
     * @param a reference ellipsoid long axis
     * @param z altitude in meters
     */
    private LatLon Geographic(EastNorth coord, double a, double e, double z) {
        double n = 0.9996 * a;
        double e2 = e * e;
        double e4 = e2 * e2;
        double e6 = e4 * e2;
        double e8 = e4 * e4;
        double C[] = {
                1.0 - e2/4.0 - 3.0*e4/64.0 - 5.0*e6/256.0 - 175.0*e8/16384.0,
                e2/8.0 + e4/48.0 + 7.0*e6/2048.0 + e8/61440.0,
                e4/768.0 + 3.0*e6/1280.0 + 559.0*e8/368640.0,
                17.0*e6/30720.0 + 283.0*e8/430080.0,
                4397.0*e8/41287680.0
        };
        double l = (coord.north() - Ys) / (n * C[0]);
        double ls = (coord.east() - Xs) / (n * C[0]);
        double l0 = l;
        double ls0 = ls;
        for(int k = 1; k < 5; k++) {
            double r = 2.0 * k * l0;
            double m = 2.0 * k * ls0;
            double em = Math.exp(m);
            double en = Math.exp(-m);
            double sr = Math.sin(r)/2.0 * (em + en);
            double sm = Math.cos(r)/2.0 * (em - en);
            l -= C[k] * sr;
            ls -= C[k] * sm;
        }
        double lon = lg0 + Math.atan(((Math.exp(ls) - Math.exp(-ls)) / 2.0) /
                Math.cos(l));
        double phi = Math.asin(Math.sin(l) /
                ((Math.exp(ls) + Math.exp(-ls)) / 2.0));
        l = Math.log(Math.tan(Math.PI/4.0 + phi/2.0));
        double lat = 2.0 * Math.atan(Math.exp(l)) - Math.PI / 2.0;
        double lt0;
        do {
            lt0 = lat;
            double s = e * Math.sin(lat);
            lat = 2.0 * Math.atan(Math.pow((1.0 + s) / (1.0 - s), e/2.0) *
                    Math.exp(l)) - Math.PI / 2.0;
        }
        while(Math.abs(lat-lt0) >= epsilon);
        //h = z;

        return new LatLon(lat, lon);
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

    /**
     * 7 parameters transformation
     * @param coord X, Y, Z in array
     * @return transformed X, Y, Z in array
     */
    private double[] sevenParametersTransformation(double Xa, double Ya, double Za){
        double Xb = tx + Xa*(1+scaleDiff) + Za*ry - Ya*rz;
        double Yb = ty + Ya*(1+scaleDiff) + Xa*rz - Za*rx;
        double Zb = tz + Za*(1+scaleDiff) + Ya*rx - Xa*ry;
        return new double[]{Xb, Yb, Zb};
    }

    /**
     * 7 parameters inverse transformation
     * @param coord X, Y, Z in array
     * @return transformed X, Y, Z in array
     */
    private double[] invSevenParametersTransformation(double Xa, double Ya, double Za){
        double Xb = (1-scaleDiff)*(-tx + Xa + ((-tz+Za)*(-ry) - (-ty+Ya)*(-rz)));
        double Yb = (1-scaleDiff)*(-ty + Ya + ((-tx+Xa)*(-rz) - (-tz+Za)*(-rx)));
        double Zb = (1-scaleDiff)*(-tz + Za + ((-ty+Ya)*(-rx) - (-tx+Xa)*(-ry)));
        return new double[]{Xb, Yb, Zb};
    }

}
