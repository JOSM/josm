// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Oblique Mercator Projection. A conformal, oblique, cylindrical projection with the cylinder
 * touching the ellipsoid (or sphere) along a great circle path (the central line). The
 * {@linkplain Mercator} and {@linkplain TransverseMercator Transverse Mercator} projections can
 * be thought of as special cases of the oblique mercator, where the central line is along the
 * equator or a meridian, respectively. The Oblique Mercator projection has been used in
 * Switzerland, Hungary, Madagascar, Malaysia, Borneo and the panhandle of Alaska.
 * <p>
 * The Oblique Mercator projection uses a (<var>U</var>,<var>V</var>) coordinate system, with the
 * <var>U</var> axis along the central line. During the forward projection, coordinates from the
 * ellipsoid are projected conformally to a sphere of constant total curvature, called the
 * "aposphere", before being projected onto the plane. The projection coordinates are further
 * convented to a (<var>X</var>,<var>Y</var>) coordinate system by rotating the calculated
 * (<var>u</var>,<var>v</var>) coordinates to give output (<var>x</var>,<var>y</var>) coordinates.
 * The rotation value is usually the same as the projection azimuth (the angle, east of north, of
 * the central line), but some cases allow a separate rotation parameter.
 * <p>
 * There are two forms of the oblique mercator, differing in the origin of their grid coordinates.
 * The Hotine Oblique Mercator (EPSG code 9812) has grid coordinates start at the intersection of
 * the central line and the equator of the aposphere.
 * The Oblique Mercator (EPSG code 9815) is the same, except the grid coordinates begin at the
 * central point (where the latitude of center and central line intersect). ESRI separates these
 * two case by appending {@code "Natural_Origin"} (for the {@code "Hotine_Oblique_Mercator"}) and
 * {@code "Center"} (for the {@code "Oblique_Mercator"}) to the projection names.
 * <p>
 * Two different methods are used to specify the central line for the oblique mercator:
 * 1) a central point and an azimuth, east of north, describing the central line and
 * 2) two points on the central line. The EPSG does not use the two point method,
 * while ESRI separates the two cases by putting {@code "Azimuth"} and {@code "Two_Point"}
 * in their projection names. Both cases use the point where the {@code "latitude_of_center"}
 * parameter crosses the central line as the projection's central point.
 * The {@linkplain #centralMeridian central meridian} is not a projection parameter,
 * and is instead calculated as the intersection between the central line and the
 * equator of the aposphere.
 * <p>
 * For the azimuth method, the central latitude cannot be &plusmn;90.0 degrees
 * and the central line cannot be at a maximum or minimum latitude at the central point.
 * In the two point method, the latitude of the first and second points cannot be
 * equal. Also, the latitude of the first point and central point cannot be
 * &plusmn;90.0 degrees. Furthermore, the latitude of the first point cannot be 0.0 and
 * the latitude of the second point cannot be -90.0 degrees. A change of
 * 10<sup>-7</sup> radians can allow calculation at these special cases. Snyder's restriction
 * of the central latitude being 0.0 has been removed, since the equations appear
 * to work correctly in this case.
 * <p>
 * Azimuth values of 0.0 and &plusmn;90.0 degrees are allowed (and used in Hungary
 * and Switzerland), though these cases would usually use a Mercator or
 * Transverse Mercator projection instead. Azimuth values &gt; 90 degrees cause
 * errors in the equations.
 * <p>
 * The oblique mercator is also called the "Rectified Skew Orthomorphic" (RSO). It appears
 * is that the only difference from the oblique mercator is that the RSO allows the rotation
 * from the (<var>U</var>,<var>V</var>) to (<var>X</var>,<var>Y</var>) coordinate system to
 * be different from the azimuth. This separate parameter is called
 * {@code "rectified_grid_angle"} (or {@code "XY_Plane_Rotation"} by ESRI) and is also
 * included in the EPSG's parameters for the Oblique Mercator and Hotine Oblique Mercator.
 * The rotation parameter is optional in all the non-two point projections and will be
 * set to the azimuth if not specified.
 * <p>
 * Projection cases and aliases implemented by the {@link ObliqueMercator} are:
 * <ul>
 *   <li>{@code Oblique_Mercator} (EPSG code 9815)<br>
 *       grid coordinates begin at the central point,
 *       has {@code "rectified_grid_angle"} parameter.</li>
 *   <li>{@code Hotine_Oblique_Mercator_Azimuth_Center} (ESRI)<br>
 *       grid coordinates begin at the central point.</li>
 *   <li>{@code Rectified_Skew_Orthomorphic_Center} (ESRI)<br>
 *       grid coordinates begin at the central point,
 *       has {@code "rectified_grid_angle"} parameter.</li>
 *
 *   <li>{@code Hotine_Oblique_Mercator} (EPSG code 9812)<br>
 *       grid coordinates begin at the interseciton of the central line and aposphere equator,
 *       has {@code "rectified_grid_angle"} parameter.</li>
 *   <li>{@code Hotine_Oblique_Mercator_Azimuth_Natural_Origin} (ESRI)<br>
 *       grid coordinates begin at the interseciton of the central line and aposphere equator.</li>
 *   <li>{@code Rectified_Skew_Orthomorphic_Natural_Origin} (ESRI)<br>
 *       grid coordinates begin at the interseciton of the central line and aposphere equator,
 *       has {@code "rectified_grid_angle"} parameter.</li>
 *
 *   <li>{@code Hotine_Oblique_Mercator_Two_Point_Center} (ESRI)<br>
 *       grid coordinates begin at the central point.</li>
 *   <li>{@code Hotine_Oblique_Mercator_Two_Point_Natural_Origin} (ESRI)<br>
 *       grid coordinates begin at the interseciton of the central line and aposphere equator.</li>
 * </ul>
 * <p>
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.ObliqueMercator
 * at the time of migration.
 * <p>
 * Note that automatic calculation of bounds is very limited for this projection,
 * since the central line can have any orientation.
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li>{@code libproj4} is available at
 *       <A HREF="http://members.bellatlantic.net/~vze2hc4d/proj4/">libproj4 Miscellanea</A><br>
 *       Relevent files are: {@code PJ_omerc.c}, {@code pj_tsfn.c},
 *       {@code pj_fwd.c}, {@code pj_inv.c} and {@code lib_proj.h}</li>
 *   <li>John P. Snyder (Map Projections - A Working Manual,
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",
 *       EPSG Guidence Note Number 7 part 2, Version 24.</li>
 *   <li>Gerald Evenden, 2004, <a href="http://members.verizon.net/~vze2hc4d/proj4/omerc.pdf">
 *       Documentation of revised Oblique Mercator</a></li>
 * </ul>
 *
 * @author Gerald I. Evenden (for original code in Proj4)
 * @author  Rueben Schulz
 *
 * @see <A HREF="http://mathworld.wolfram.com/MercatorProjection.html">Oblique Mercator projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/hotine_oblique_mercator.html">"hotine_oblique_mercator" on RemoteSensing.org</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/oblique_mercator.html">"oblique_mercator" on RemoteSensing.org</A>
 */
public class ObliqueMercator extends AbstractProj implements ICentralMeridianProvider {

    /**
     * Maximum difference allowed when comparing real numbers.
     */
    private static final double EPSILON = 1E-6;

    /**
     * Maximum difference allowed when comparing latitudes.
     */
    private static final double EPSILON_LATITUDE = 1E-10;

    //////
    //////    Map projection parameters. The following are NOT used by the transformation
    //////    methods, but are stored in order to restitute them in WKT formatting.  They
    //////    are made visible ('protected' access) for documentation purpose and because
    //////    they are user-supplied parameters, not derived coefficients.
    //////

    /**
     * The azimuth of the central line passing through the centre of the projection, in radians.
     */
    protected double azimuth;

    /**
     * The rectified bearing of the central line, in radians. This is equals to the
     * {@linkplain #azimuth} if the parameter value is not set.
     */
    protected double rectifiedGridAngle;

    //////
    //////    Map projection coefficients computed from the above parameters.
    //////    They are the fields used for coordinate transformations.
    //////

    /**
     * Constants used in the transformation.
     */
    private double b, g;

    /**
     * Convenience value equal to {@code a} / {@link #b}.
     */
    private double arb;

    /**
     * Convenience value equal to {@code a}&times;{@link #b}.
     */
    private double ab;

    /**
     * Convenience value equal to {@link #b} / {@code a}.
     */
    private double bra;

    /**
     * <var>v</var> values when the input latitude is a pole.
     */
    private double vPoleN, vPoleS;

    /**
     * Sine and Cosine values for gamma0 (the angle between the meridian
     * and central line at the intersection between the central line and
     * the Earth equator on aposphere).
     */
    private double singamma0, cosgamma0;

    /**
     * Sine and Cosine values for the rotation between (U,V) and
     * (X,Y) coordinate systems
     */
    private double sinrot, cosrot;

    /**
     * <var>u</var> value (in (U,V) coordinate system) of the central point. Used in
     * the oblique mercator case. The <var>v</var> value of the central point is 0.0.
     */
    private double uc;

    /**
     * Central longitude in <u>radians</u>. Default value is 0, the Greenwich meridian.
     * This is called '<var>lambda0</var>' in Snyder.
     */
    protected double centralMeridian;

    /**
     * A reference point, which is known to be on the central line.
     */
    private LatLon referencePoint;

    @Override
    public String getName() {
        return tr("Oblique Mercator");
    }

    @Override
    public String getProj4Id() {
        return "omerc";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        boolean twoPoint = params.alpha == null;

        double latCenter = 0;
        if (params.lat0 != null) {
            latCenter = Utils.toRadians(params.lat0);
        }

        final double com = Math.sqrt(1.0 - e2);
        double sinph0 = Math.sin(latCenter);
        double cosph0 = Math.cos(latCenter);
        final double con = 1. - e2 * sinph0 * sinph0;
        double temp = cosph0 * cosph0;
        b = Math.sqrt(1.0 + e2 * (temp * temp) / (1.0 - e2));
        double a = b * com / con;
        final double d = b * com / (cosph0 * Math.sqrt(con));
        double f = d * d - 1.0;
        if (f < 0.0) {
            f = 0.0;
        } else {
            f = Math.sqrt(f);
            if (latCenter < 0.0) {
                f = -f;
            }
        }
        g = f += d;
        g = f * Math.pow(tsfn(latCenter, sinph0), b);

        /*
         * Computes the constants that depend on the "twoPoint" vs "azimuth" case. In the
         * two points case, we compute them from (LAT_OF_1ST_POINT, LONG_OF_1ST_POINT) and
         * (LAT_OF_2ND_POINT, LONG_OF_2ND_POINT).  For the "azimuth" case, we compute them
         * from LONGITUDE_OF_CENTRE and AZIMUTH.
         */
        final double gamma0;
        Double lonCenter = null;
        if (twoPoint) {
            if (params.lon1 == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lon_1"));
            if (params.lat1 == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_1"));
            if (params.lon2 == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lon_2"));
            if (params.lat2 == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_2"));
            referencePoint = new LatLon(params.lat1, params.lat2);
            double lon1 = Utils.toRadians(params.lon1);
            double lat1 = Utils.toRadians(params.lat1);
            double lon2 = Utils.toRadians(params.lon2);
            double lat2 = Utils.toRadians(params.lat2);

            if (Math.abs(lat1 - lat2) <= EPSILON ||
                Math.abs(lat1) <= EPSILON ||
                Math.abs(Math.abs(lat1) - Math.PI / 2) <= EPSILON ||
                Math.abs(Math.abs(latCenter) - Math.PI / 2) <= EPSILON ||
                Math.abs(Math.abs(lat2) - Math.PI / 2) <= EPSILON) {
                throw new ProjectionConfigurationException(
                    tr("Unsuitable parameters ''{0}'' and ''{1}'' for two point method.", "lat_1", "lat_2"));
            }
            /*
             * The coefficients for the "two points" case.
             */
            final double h = Math.pow(tsfn(lat1, Math.sin(lat1)), b);
            final double l = Math.pow(tsfn(lat2, Math.sin(lat2)), b);
            final double fp = g / h;
            final double p = (l - h) / (l + h);
            double j = g * g;
            j = (j - l * h) / (j + l * h);
            double diff = lon1 - lon2;
            if (diff < -Math.PI) {
                lon2 -= 2.0 * Math.PI;
            } else if (diff > Math.PI) {
                lon2 += 2.0 * Math.PI;
            }
            centralMeridian = normalizeLonRad(0.5 * (lon1 + lon2) -
                     Math.atan(j * Math.tan(0.5 * b * (lon1 - lon2)) / p) / b);
            gamma0 = Math.atan(2.0 * Math.sin(b * normalizeLonRad(lon1 - centralMeridian)) /
                     (fp - 1.0 / fp));
            azimuth = Math.asin(d * Math.sin(gamma0));
            rectifiedGridAngle = azimuth;
        } else {
            if (params.lonc == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lonc"));
            if (params.lat0 == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
            if (params.alpha == null)
                throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "alpha"));
            referencePoint = new LatLon(params.lat0, params.lonc);

            lonCenter = Utils.toRadians(params.lonc);
            azimuth = Utils.toRadians(params.alpha);
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            if ((azimuth > -1.5*Math.PI && azimuth < -0.5*Math.PI) ||
                (azimuth >  0.5*Math.PI && azimuth <  1.5*Math.PI)) {
                throw new ProjectionConfigurationException(
                        tr("Illegal value for parameter ''{0}'': {1}", "alpha", Double.toString(params.alpha)));
            }
            // CHECKSTYLE.ON: SingleSpaceSeparator
            if (params.gamma != null) {
                rectifiedGridAngle = Utils.toRadians(params.gamma);
            } else {
                rectifiedGridAngle = azimuth;
            }
            gamma0 = Math.asin(Math.sin(azimuth) / d);
            // Check for asin(+-1.00000001)
            temp = 0.5 * (f - 1.0 / f) * Math.tan(gamma0);
            if (Math.abs(temp) > 1.0) {
                if (Math.abs(Math.abs(temp) - 1.0) > EPSILON) {
                    throw new ProjectionConfigurationException(tr("error in initialization"));
                }
                temp = (temp > 0) ? 1.0 : -1.0;
            }
            centralMeridian = lonCenter - Math.asin(temp) / b;
        }

        /*
         * More coefficients common to all kind of oblique mercator.
         */
        singamma0 = Math.sin(gamma0);
        cosgamma0 = Math.cos(gamma0);
        sinrot = Math.sin(rectifiedGridAngle);
        cosrot = Math.cos(rectifiedGridAngle);
        arb = a / b;
        ab = a * b;
        bra = b / a;
        vPoleN = arb * Math.log(Math.tan(0.5 * (Math.PI/2.0 - gamma0)));
        vPoleS = arb * Math.log(Math.tan(0.5 * (Math.PI/2.0 + gamma0)));
        boolean hotine = params.no_off != null && params.no_off;
        if (hotine) {
            uc = 0.0;
        } else {
            if (Math.abs(Math.abs(azimuth) - Math.PI/2.0) < EPSILON_LATITUDE) {
                // lonCenter == null in twoPoint, but azimuth cannot be 90 here (lat1 != lat2)
                if (lonCenter == null) {
                    throw new ProjectionConfigurationException("assertion error");
                }
                uc = a * (lonCenter - centralMeridian);
            } else {
                double uC = Math.abs(arb * Math.atan2(Math.sqrt(d * d - 1.0), Math.cos(azimuth)));
                if (latCenter < 0.0) {
                    uC = -uC;
                }
                this.uc = uC;
            }
        }
    }

    private static double normalizeLonRad(double a) {
        return Utils.toRadians(LatLon.normalizeLon(Utils.toDegrees(a)));
    }

    @Override
    public double[] project(double y, double x) {
        double u, v;
        if (Math.abs(Math.abs(y) - Math.PI/2.0) > EPSILON) {
            double q = g / Math.pow(tsfn(y, Math.sin(y)), b);
            double temp = 1.0 / q;
            double s = 0.5 * (q - temp);
            double v2 = Math.sin(b * x);
            double u2 = (s * singamma0 - v2 * cosgamma0) / (0.5 * (q + temp));
            if (Math.abs(Math.abs(u2) - 1.0) < EPSILON) {
                v = 0; // this is actually an error and should be reported to the caller somehow
            } else {
                v = 0.5 * arb * Math.log((1.0 - u2) / (1.0 + u2));
            }
            temp = Math.cos(b * x);
            if (Math.abs(temp) < EPSILON_LATITUDE) {
                u = ab * x;
            } else {
                u = arb * Math.atan2(s * cosgamma0 + v2 * singamma0, temp);
            }
        } else {
            v = y > 0 ? vPoleN : vPoleS;
            u = arb * y;
        }
        u -= uc;
        x = v * cosrot + u * sinrot;
        y = u * cosrot - v * sinrot;
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        double v = x * cosrot - y * sinrot;
        double u = y * cosrot + x * sinrot + uc;
        double qp = Math.exp(-bra * v);
        double temp = 1.0 / qp;
        double sp = 0.5 * (qp - temp);
        double vp = Math.sin(bra * u);
        double up = (vp * cosgamma0 + sp * singamma0) / (0.5 * (qp + temp));
        if (Math.abs(Math.abs(up) - 1.0) < EPSILON) {
            return new double[] {
                up < 0.0 ? -(Math.PI / 2.0) : (Math.PI / 2.0),
                0.0};
        } else {
            return new double[] {
                cphi2(Math.pow(g / Math.sqrt((1. + up) / (1. - up)), 1.0 / b)), //calculate t
                -Math.atan2(sp * cosgamma0 - vp * singamma0, Math.cos(bra * u)) / b};
        }
    }

    @Override
    public Bounds getAlgorithmBounds() {
        // The central line of this projection can be oriented in any direction.
        // Moreover, the projection doesn't work too well very far off the central line.
        // This makes it hard to choose proper bounds automatically.
        //
        // We return a small box around a reference point. This default box is
        // probably too small for most applications. If this is the case, the
        // bounds should be configured explicitly.
        double lat = referencePoint.lat();
        double dLat = 3.0;
        double lon = referencePoint.lon() - Utils.toDegrees(centralMeridian);
        double dLon = 3.0;
        return new Bounds(lat - dLat, lon - dLon, lat + dLat, lon + dLon, false);
    }

    @Override
    public double getCentralMeridian() {
        return Utils.toDegrees(centralMeridian);
    }
}
