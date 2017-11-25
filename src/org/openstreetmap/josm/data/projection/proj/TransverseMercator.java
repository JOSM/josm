// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Transverse Mercator Projection (EPSG code 9807). This
 * is a cylindrical projection, in which the cylinder has been rotated 90°.
 * Instead of being tangent to the equator (or to an other standard latitude),
 * it is tangent to a central meridian. Deformation are more important as we
 * are going futher from the central meridian. The Transverse Mercator
 * projection is appropriate for region wich have a greater extent north-south
 * than east-west.
 * <p>
 *
 * The elliptical equations used here are series approximations, and their accuracy
 * decreases as points move farther from the central meridian of the projection.
 * The forward equations here are accurate to a less than a mm &plusmn;10 degrees from
 * the central meridian, a few mm &plusmn;15 degrees from the
 * central meridian and a few cm &plusmn;20 degrees from the central meridian.
 * The spherical equations are not approximations and should always give the
 * correct values.
 * <p>
 *
 * There are a number of versions of the transverse mercator projection
 * including the Universal (UTM) and Modified (MTM) Transverses Mercator
 * projections. In these cases the earth is divided into zones. For the UTM
 * the zones are 6 degrees wide, numbered from 1 to 60 proceeding east from
 * 180 degrees longitude, and between lats 84 degrees North and 80
 * degrees South. The central meridian is taken as the center of the zone
 * and the latitude of origin is the equator. A scale factor of 0.9996 and
 * false easting of 500000m is used for all zones and a false northing of 10000000m
 * is used for zones in the southern hemisphere.
 * <p>
 *
 * NOTE: formulas used below are not those of Snyder, but rather those
 *       from the {@code proj4} package of the USGS survey, which
 *       have been reproduced verbatim. USGS work is acknowledged here.
 * <p>
 *
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.TransverseMercator
 * at the time of migration.
 * <p>
 * The non-standard parameter <code>gamma</code> has been added as a method
 * to rotate the projected coordinates by a certain angle (clockwise, see
 * {@link ObliqueMercator}).
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li> Proj-4.4.6 available at <A HREF="http://www.remotesensing.org/proj">www.remotesensing.org/proj</A><br>
 *        Relevent files are: {@code PJ_tmerc.c}, {@code pj_mlfn.c}, {@code pj_fwd.c} and {@code pj_inv.c}.</li>
 *   <li> John P. Snyder (Map Projections - A Working Manual,
 *        U.S. Geological Survey Professional Paper 1395, 1987).</li>
 *   <li> "Coordinate Conversions and Transformations including Formulas",
 *        EPSG Guidence Note Number 7, Version 19.</li>
 * </ul>
 *
 * @author André Gosselin
 * @author Martin Desruisseaux (PMO, IRD)
 * @author Rueben Schulz
 *
 * @see <A HREF="http://mathworld.wolfram.com/MercatorProjection.html">Transverse Mercator projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">"Transverse_Mercator" on RemoteSensing.org</A>
 */
public class TransverseMercator extends AbstractProj {

    /** Earth emispheres **/
    public enum Hemisphere {
        /** North emisphere */
        North,
        /** South emisphere */
        South
    }

    /**
     * Contants used for the forward and inverse transform for the eliptical
     * case of the Transverse Mercator.
     */
    private static final double FC1 = 1.00000000000000000000000,  // 1/1
                                FC2 = 0.50000000000000000000000,  // 1/2
                                FC3 = 0.16666666666666666666666,  // 1/6
                                FC4 = 0.08333333333333333333333,  // 1/12
                                FC5 = 0.05000000000000000000000,  // 1/20
                                FC6 = 0.03333333333333333333333,  // 1/30
                                FC7 = 0.02380952380952380952380,  // 1/42
                                FC8 = 0.01785714285714285714285;  // 1/56

    /**
     * Maximum difference allowed when comparing real numbers.
     */
    private static final double EPSILON = 1E-6;

    /**
     * A derived quantity of excentricity, computed by <code>e'² = (a²-b²)/b² = es/(1-es)</code>
     * where <var>a</var> is the semi-major axis length and <var>b</var> is the semi-minor axis
     * length.
     */
    private double eb2;

    /**
     * Latitude of origin in <u>radians</u>. Default value is 0, the equator.
     * This is called '<var>phi0</var>' in Snyder.
     * <p>
     * <strong>Consider this field as final</strong>. It is not final only
     * because some classes need to modify it at construction time.
     */
    protected double latitudeOfOrigin;

    /**
     * Meridian distance at the {@code latitudeOfOrigin}.
     * Used for calculations for the ellipsoid.
     */
    private double ml0;

    /**
     * The rectified bearing of the central line, in radians.
     */
    protected double rectifiedGridAngle;

    /**
     * Sine and Cosine values for the coordinate system rotation angle
     */
    private double sinrot, cosrot;

    @Override
    public String getName() {
        return tr("Transverse Mercator");
    }

    @Override
    public String getProj4Id() {
        return "tmerc";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        CheckParameterUtil.ensureParameterNotNull(params, "params");
        CheckParameterUtil.ensureParameterNotNull(params.ellps, "params.ellps");
        eb2 = params.ellps.eb2;
        latitudeOfOrigin = params.lat0 == null ? 0 : Utils.toRadians(params.lat0);
        ml0 = mlfn(latitudeOfOrigin, Math.sin(latitudeOfOrigin), Math.cos(latitudeOfOrigin));

        if (params.gamma != null) {
                rectifiedGridAngle = Utils.toRadians(params.gamma);
        } else {
                rectifiedGridAngle = 0.0;
        }
        sinrot = Math.sin(rectifiedGridAngle);
        cosrot = Math.cos(rectifiedGridAngle);

    }

    @Override
    public double[] project(double y, double x) {
        double sinphi = Math.sin(y);
        double cosphi = Math.cos(y);
        double u, v;

        double t = (Math.abs(cosphi) > EPSILON) ? sinphi/cosphi : 0;
        t *= t;
        double al = cosphi*x;
        double als = al*al;
        al /= Math.sqrt(1.0 - e2 * sinphi*sinphi);
        double n = eb2 * cosphi*cosphi;

        /* NOTE: meridinal distance at latitudeOfOrigin is always 0 */
        y = mlfn(y, sinphi, cosphi) - ml0 +
            sinphi * al * x *
            FC2 * (1.0 +
            FC4 * als * (5.0 - t + n*(9.0 + 4.0*n) +
            FC6 * als * (61.0 + t * (t - 58.0) + n*(270.0 - 330.0*t) +
            FC8 * als * (1385.0 + t * (t*(543.0 - t) - 3111.0)))));

        x = al*(FC1 + FC3 * als*(1.0 - t + n +
            FC5 * als * (5.0 + t*(t - 18.0) + n*(14.0 - 58.0*t) +
            FC7 * als * (61.0+ t*(t*(179.0 - t) - 479.0)))));

        u = y;
        v = x;
        x = v * cosrot + u * sinrot;
        y = u * cosrot - v * sinrot;

        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        double v = x * cosrot - y * sinrot;
        double u = y * cosrot + x * sinrot;
        x = v;
        y = u;

        double phi = invMlfn(ml0 + y);

        if (Math.abs(phi) >= Math.PI/2) {
            y = y < 0.0 ? -(Math.PI/2) : (Math.PI/2);
            x = 0.0;
        } else {
            double sinphi = Math.sin(phi);
            double cosphi = Math.cos(phi);
            double t = (Math.abs(cosphi) > EPSILON) ? sinphi/cosphi : 0.0;
            double n = eb2 * cosphi*cosphi;
            double con = 1.0 - e2 * sinphi*sinphi;
            double d = x * Math.sqrt(con);
            con *= t;
            t *= t;
            double ds = d*d;

            y = phi - (con*ds / (1.0 - e2)) *
                FC2 * (1.0 - ds *
                FC4 * (5.0 + t*(3.0 - 9.0*n) + n*(1.0 - 4*n) - ds *
                FC6 * (61.0 + t*(90.0 - 252.0*n + 45.0*t) + 46.0*n - ds *
                FC8 * (1385.0 + t*(3633.0 + t*(4095.0 + 1574.0*t))))));

            x = d*(FC1 - ds * FC3 * (1.0 + 2.0*t + n -
                ds*FC5*(5.0 + t*(28.0 + 24* t + 8.0*n) + 6.0*n -
                ds*FC7*(61.0 + t*(662.0 + t*(1320.0 + 720.0*t))))))/cosphi;
        }
        return new double[] {y, x};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -7, 89, 7, false);
    }

    /**
     * Determines the UTM zone of a given lat/lon.
     * @param ll lat/lon to locate in the UTM grid.
     * @return the UTM zone of {@code ll}
     * @since 13167
     */
    public static Pair<Integer, Hemisphere> locateUtmZone(LatLon ll) {
        return new Pair<>((int) Math.floor((ll.lon() + 180d) / 6d) + 1,
                ll.lat() > 0 ? Hemisphere.North : Hemisphere.South);
    }
}
