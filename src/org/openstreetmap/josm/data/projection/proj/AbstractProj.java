// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Abstract base class providing utilities for implementations of the Proj
 * interface.
 *
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.MapProjection
 * at the time of migration.
 * <p>
 *
 * @author André Gosselin
 * @author Martin Desruisseaux (PMO, IRD)
 * @author Rueben Schulz
*/
public abstract class AbstractProj implements Proj {

    /**
     * Maximum number of iterations for iterative computations.
     */
    private static final int MAXIMUM_ITERATIONS = 15;

    /**
     * Difference allowed in iterative computations.
     */
    private static final double ITERATION_TOLERANCE = 1E-10;

    /**
     * Relative iteration precision used in the <code>mlfn</code> method
     */
    private static final double MLFN_TOL = 1E-11;

    /**
     * Constants used to calculate {@link #en0}, {@link #en1},
     * {@link #en2}, {@link #en3}, {@link #en4}.
     */
    private static final double C00 = 1.0;
    private static final double C02 = 0.25;
    private static final double C04 = 4.6875E-02;
    private static final double C06 = 1.953125E-02;
    private static final double C08 = 1.068115234375E-02;
    private static final double C22 = 0.75;
    private static final double C44 = 4.6875E-01;
    private static final double C46 = 1.30208333333333E-02;
    private static final double C48 = 7.12076822916667E-03;
    private static final double C66 = 3.64583333333333E-01;
    private static final double C68 = 5.69661458333333E-03;
    private static final double C88 = 3.076171875E-01;

    /**
     * Constant needed for the <code>mlfn</code> method.
     * Setup at construction time.
     */
    protected double en0, en1, en2, en3, en4;

    /**
     * Ellipsoid excentricity, equals to <code>sqrt({@link #e2 excentricity squared})</code>.
     * Value 0 means that the ellipsoid is spherical.
     *
     * @see #e2
     */
    protected double e;

    /**
     * The square of excentricity: e² = (a²-b²)/a² where
     * <var>e</var> is the excentricity,
     * <var>a</var> is the semi major axis length and
     * <var>b</var> is the semi minor axis length.
     *
     * @see #e
     */
    protected double e2;

    /**
     * is ellipsoid spherical?
     * @see Ellipsoid#spherical
     */
    protected boolean spherical;

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        CheckParameterUtil.ensureParameterNotNull(params, "params");
        CheckParameterUtil.ensureParameterNotNull(params.ellps, "params.ellps");
        e2 = params.ellps.e2;
        e = params.ellps.e;
        spherical = params.ellps.spherical;
        //  Compute constants for the mlfn
        double t;
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        en0 = C00 - e2  *  (C02 + e2  *
             (C04 + e2  *  (C06 + e2  * C08)));
        en1 =       e2  *  (C22 - e2  *
             (C04 + e2  *  (C06 + e2  * C08)));
        en2 =  (t = e2  *         e2) *
             (C44 - e2  *  (C46 + e2  * C48));
        en3 = (t *= e2) *  (C66 - e2  * C68);
        en4 =   t * e2  *  C88;
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }

    @Override
    public boolean isGeographic() {
        return false;
    }

    /**
     * Calculates the meridian distance. This is the distance along the central
     * meridian from the equator to {@code phi}. Accurate to &lt; 1e-5 meters
     * when used in conjunction with typical major axis values.
     *
     * @param phi latitude to calculate meridian distance for.
     * @param sphi sin(phi).
     * @param cphi cos(phi).
     * @return meridian distance for the given latitude.
     */
    protected final double mlfn(final double phi, double sphi, double cphi) {
        cphi *= sphi;
        sphi *= sphi;
        return en0 * phi - cphi *
              (en1 + sphi *
              (en2 + sphi *
              (en3 + sphi *
              en4)));
    }

    /**
     * Calculates the latitude ({@code phi}) from a meridian distance.
     * Determines phi to TOL (1e-11) radians, about 1e-6 seconds.
     *
     * @param arg meridian distance to calculate latitude for.
     * @return the latitude of the meridian distance.
     * @throws RuntimeException if the itteration does not converge.
     */
    protected final double invMlfn(double arg) {
        double s, t, phi, k = 1.0/(1.0 - e2);
        int i;
        phi = arg;
        for (i = MAXIMUM_ITERATIONS; true;) { // rarely goes over 5 iterations
            if (--i < 0) {
                throw new IllegalStateException("Too many iterations");
            }
            s = Math.sin(phi);
            t = 1.0 - e2 * s * s;
            t = (mlfn(phi, s, Math.cos(phi)) - arg) * (t * Math.sqrt(t)) * k;
            phi -= t;
            if (Math.abs(t) < MLFN_TOL) {
                return phi;
            }
        }
    }

    /**
     * Tolerant asin that will just return the limits of its output range if the input is out of range
     * @param v the value whose arc sine is to be returned.
     * @return the arc sine of the argument.
     */
    protected final double aasin(double v) {
        double av = Math.abs(v);
        if (av >= 1.) {
            return (v < 0. ? -Math.PI / 2 : Math.PI / 2);
        }
        return Math.asin(v);
    }

    // Iteratively solve equation (7-9) from Snyder.
    final double cphi2(final double ts) {
        final double eccnth = 0.5 * e;
        double phi = (Math.PI/2) - 2.0 * Math.atan(ts);
        for (int i = 0; i < MAXIMUM_ITERATIONS; i++) {
            final double con = e * Math.sin(phi);
            final double dphi = (Math.PI/2) - 2.0*Math.atan(ts * Math.pow((1-con)/(1+con), eccnth)) - phi;
            phi += dphi;
            if (Math.abs(dphi) <= ITERATION_TOLERANCE) {
                return phi;
            }
        }
        throw new IllegalStateException("no convergence for ts="+ts);
    }

    /**
     * Computes function <code>f(s,c,e²) = c/sqrt(1 - s²&times;e²)</code> needed for the true scale
     * latitude (Snyder 14-15), where <var>s</var> and <var>c</var> are the sine and cosine of
     * the true scale latitude, and <var>e²</var> is the {@linkplain #e2 eccentricity squared}.
     * @param s sine of the true scale latitude
     * @param c cosine of the true scale latitude
     * @return <code>c/sqrt(1 - s²&times;e²)</code>
     */
    final double msfn(final double s, final double c) {
        return c / Math.sqrt(1.0 - (s*s) * e2);
    }

    /**
     * Computes function (15-9) and (9-13) from Snyder.
     * Equivalent to negative of function (7-7).
     * @param lat the latitude
     * @param sinlat sine of the latitude
     * @return auxiliary value computed from <code>lat</code> and <code>sinlat</code>
     */
    final double tsfn(final double lat, double sinlat) {
        sinlat *= e;
        // NOTE: change sign to get the equivalent of Snyder (7-7).
        return Math.tan(0.5 * (Math.PI/2 - lat)) / Math.pow((1 - sinlat) / (1 + sinlat), 0.5*e);
    }
}
