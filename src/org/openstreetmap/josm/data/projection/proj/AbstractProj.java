// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

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
     * Relative iteration precision used in the <code>mlfn<code> method
     */
    private static final double MLFN_TOL = 1E-11;

    /**
     * Constants used to calculate {@link #en0}, {@link #en1},
     * {@link #en2}, {@link #en3}, {@link #en4}.
     */
    private static final double C00= 1.0,
                                C02= 0.25,
                                C04= 0.046875,
                                C06= 0.01953125,
                                C08= 0.01068115234375,
                                C22= 0.75,
                                C44= 0.46875,
                                C46= 0.01302083333333333333,
                                C48= 0.00712076822916666666,
                                C66= 0.36458333333333333333,
                                C68= 0.00569661458333333333,
                                C88= 0.3076171875;

    /**
     * Constant needed for the <code>mlfn<code> method.
     * Setup at construction time.
     */
    protected double en0,en1,en2,en3,en4;

    /**
     * The square of excentricity: e² = (a²-b²)/a² where
     * <var>e</var> is the {@linkplain #excentricity excentricity},
     * <var>a</var> is the {@linkplain #semiMajor semi major} axis length and
     * <var>b</var> is the {@linkplain #semiMinor semi minor} axis length.
     */
    protected double e2;

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        e2 = params.ellps.e2;
        //  Compute constants for the mlfn
        double t;
        en0 = C00 - e2  *  (C02 + e2  *
             (C04 + e2  *  (C06 + e2  * C08)));
        en1 =       e2  *  (C22 - e2  *
             (C04 + e2  *  (C06 + e2  * C08)));
        en2 =  (t = e2  *         e2) *
             (C44 - e2  *  (C46 + e2  * C48));
        en3 = (t *= e2) *  (C66 - e2  * C68);
        en4 =   t * e2  *  C88;
    }

    /**
     * Calculates the meridian distance. This is the distance along the central
     * meridian from the equator to {@code phi}. Accurate to < 1e-5 meters
     * when used in conjuction with typical major axis values.
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
              (en4))));
    }

    /**
     * Calculates the latitude ({@code phi}) from a meridian distance.
     * Determines phi to TOL (1e-11) radians, about 1e-6 seconds.
     *
     * @param arg meridian distance to calulate latitude for.
     * @return the latitude of the meridian distance.
     * @throws RuntimeException if the itteration does not converge.
     */
    protected final double inv_mlfn(double arg) {
        double s, t, phi, k = 1.0/(1.0 - e2);
        int i;
        phi = arg;
        for (i=MAXIMUM_ITERATIONS; true;) { // rarely goes over 5 iterations
            if (--i < 0) {
                throw new RuntimeException("Too many iterations");
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

    public static double normalizeLon(double lon) {
        if (lon >= - Math.PI && lon <= Math.PI)
            return lon;
        else {
            lon = lon % (2 * Math.PI);
            if (lon > Math.PI) {
                return lon - 2 * Math.PI;
            } else if (lon < -Math.PI) {
                return lon + 2 * Math.PI;
            }
            return lon;
        }
    }
}
