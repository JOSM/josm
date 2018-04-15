// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Albers Equal Area Projection (EPSG code 9822). This is a conic projection with parallels being
 * unequally spaced arcs of concentric circles, more closely spaced at north and south edges of the
 * map. Merideans are equally spaced radii of the same circles and intersect parallels at right
 * angles. As the name implies, this projection minimizes distortion in areas.
 * <p>
 * The {@code "standard_parallel_2"} parameter is optional and will be given the same value as
 * {@code "standard_parallel_1"} if not set (creating a 1 standard parallel projection).
 * <p>
 * <b>NOTE:</b>
 * formulae used below are from a port, to Java, of the {@code proj4}
 * package of the USGS survey. USGS work is acknowledged here.
 * <p>
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.AlbersEqualArea
 * at the time of migration.
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li> Proj-4.4.7 available at <A HREF="http://www.remotesensing.org/proj">www.remotesensing.org/proj</A><br>
 *        Relevent files are: PJ_aea.c, pj_fwd.c and pj_inv.c </li>
 *   <li> John P. Snyder (Map Projections - A Working Manual,
 *        U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li> "Coordinate Conversions and Transformations including Formulas",
 *        EPSG Guidence Note Number 7, Version 19.</li>
 * </ul>
 *
 * @author Gerald I. Evenden (for original code in Proj4)
 * @author Rueben Schulz
 *
 * @see <A HREF="http://mathworld.wolfram.com/AlbersEqual-AreaConicProjection.html">Albers Equal-Area Conic Projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/albers_equal_area_conic.html">"Albers_Conic_Equal_Area" on RemoteSensing.org</A>
 * @see <A HREF="http://srmwww.gov.bc.ca/gis/bceprojection.html">British Columbia Albers Standard Projection</A>
 *
 * @since 9419
 */
public class AlbersEqualArea extends AbstractProj {

    /**
     * Maximum number of iterations for iterative computations.
     */
    private static final int MAXIMUM_ITERATIONS = 15;

    /**
     * Difference allowed in iterative computations.
     */
    private static final double ITERATION_TOLERANCE = 1E-10;

    /**
     * Maximum difference allowed when comparing real numbers.
     */
    private static final double EPSILON = 1E-6;

    /**
     * Constants used by the spherical and elliptical Albers projection.
     */
    private double n, n2, c, rho0;

    /**
     * An error condition indicating iteration will not converge for the
     * inverse ellipse. See Snyder (14-20)
     */
    private double ec;

    @Override
    public String getName() {
        return tr("Albers Equal Area");
    }

    @Override
    public String getProj4Id() {
        return "aea";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        if (params.lat1 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_1"));

        double lat0 = Utils.toRadians(params.lat0);
        // Standards parallels in radians.
        double phi1 = Utils.toRadians(params.lat1);
        double phi2 = params.lat2 == null ? phi1 : Utils.toRadians(params.lat2);

        // Compute Constants
        if (Math.abs(phi1 + phi2) < EPSILON) {
            throw new ProjectionConfigurationException(tr("standard parallels are opposite"));
        }
        double sinphi = Math.sin(phi1);
        double cosphi = Math.cos(phi1);
        double n = sinphi;
        boolean secant = Math.abs(phi1 - phi2) >= EPSILON;
        double m1 = msfn(sinphi, cosphi);
        double q1 = qsfn(sinphi);
        if (secant) { // secant cone
            sinphi = Math.sin(phi2);
            cosphi = Math.cos(phi2);
            double m2 = msfn(sinphi, cosphi);
            double q2 = qsfn(sinphi);
            n = (m1 * m1 - m2 * m2) / (q2 - q1);
        }
        c = m1 * m1 + n * q1;
        rho0 = Math.sqrt(c - n * qsfn(Math.sin(lat0))) /n;
        ec = 1.0 - .5 * (1.0-e2) *
             Math.log((1.0 - e) / (1.0 + e)) / e;
        n2 = n * n;
        this.n = n;
    }

    @Override
    public double[] project(double y, double x) {
        double theta = n * x;
        double rho = c - (spherical ? n2 * Math.sin(y) : n * qsfn(Math.sin(y)));
        if (rho < 0.0) {
            if (rho > -EPSILON) {
                rho = 0.0;
            } else {
                throw new AssertionError();
            }
        }
        rho = Math.sqrt(rho) / n;
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        y = rho0 - rho * Math.cos(theta);
        x =        rho * Math.sin(theta);
        // CHECKSTYLE.ON: SingleSpaceSeparator
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        y = rho0 - y;
        double rho = Math.hypot(x, y);
        if (rho > EPSILON) {
            if (n < 0.0) {
                rho = -rho;
                x = -x;
                y = -y;
            }
            x = Math.atan2(x, y) / n;
            y = rho * n;
            if (spherical) {
                y = aasin((c - y*y) / n2);
            } else {
                y = (c - y*y) / n;
                if (Math.abs(y) <= ec) {
                    y = phi1(y);
                } else {
                    y = (y < 0.0) ? -Math.PI/2.0 : Math.PI/2.0;
                }
            }
        } else {
            x = 0.0;
            y = n > 0.0 ? Math.PI/2.0 : -Math.PI/2.0;
        }
        return new double[] {y, x};
    }

    /**
     * Iteratively solves equation (3-16) from Snyder.
     *
     * @param qs arcsin(q/2), used in the first step of iteration
     * @return the latitude
     */
    public double phi1(final double qs) {
        final double toneEs = 1 - e2;
        double phi = Math.asin(0.5 * qs);
        if (e < EPSILON) {
            return phi;
        }
        for (int i = 0; i < MAXIMUM_ITERATIONS; i++) {
            final double sinpi = Math.sin(phi);
            final double cospi = Math.cos(phi);
            final double con = e * sinpi;
            final double com = 1.0 - con*con;
            final double dphi = 0.5 * com*com / cospi *
                    (qs/toneEs - sinpi / com + 0.5/e * Math.log((1. - con) / (1. + con)));
            phi += dphi;
            if (Math.abs(dphi) <= ITERATION_TOLERANCE) {
                return phi;
            }
        }
        throw new IllegalStateException("no convergence for qs="+qs);
    }

    /**
     * Calculates q, Snyder equation (3-12)
     *
     * @param sinphi sin of the latitude q is calculated for
     * @return q from Snyder equation (3-12)
     */
    private double qsfn(final double sinphi) {
        final double oneEs = 1 - e2;
        if (e >= EPSILON) {
            final double con = e * sinphi;
            return oneEs * (sinphi / (1. - con*con) -
                    (0.5/e) * Math.log((1.-con) / (1.+con)));
        } else {
            return sinphi + sinphi;
        }
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -180, 89, 180, false);
    }
}
