// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Transverse Mercator projection.
 *
 * @author Dirk Stöcker
 * code based on JavaScript from Chuck Taylor
 *
 */
public class TransverseMercator implements Proj {

    protected double a, b;

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
        this.a = params.ellps.a;
        this.b = params.ellps.b;
    }

    /**
     * Converts a latitude/longitude pair to x and y coordinates in the
     * Transverse Mercator projection.  Note that Transverse Mercator is not
     * the same as UTM; a scale factor is required to convert between them.
     *
     * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
     * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
     *
     * @param phi Latitude of the point, in radians
     * @param lambda Longitude of the point, in radians
     * @return A 2-element array containing the x and y coordinates
     *         of the computed point
     */
    @Override
    public double[] project(double phi, double lambda) {

        /* Precalculate ep2 */
        double ep2 = (pow(a, 2.0) - pow(b, 2.0)) / pow(b, 2.0);

        /* Precalculate nu2 */
        double nu2 = ep2 * pow(cos(phi), 2.0);

        /* Precalculate N / a */
        double N_a = a / (b * sqrt(1 + nu2));

        /* Precalculate t */
        double t = tan(phi);
        double t2 = t * t;

        /* Precalculate l */
        double l = lambda;

        /* Precalculate coefficients for l**n in the equations below
           so a normal human being can read the expressions for easting
           and northing
           -- l**1 and l**2 have coefficients of 1.0 */
        double l3coef = 1.0 - t2 + nu2;

        double l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);

        double l5coef = 5.0 - 18.0 * t2 + (t2 * t2) + 14.0 * nu2
        - 58.0 * t2 * nu2;

        double l6coef = 61.0 - 58.0 * t2 + (t2 * t2) + 270.0 * nu2
        - 330.0 * t2 * nu2;

        double l7coef = 61.0 - 479.0 * t2 + 179.0 * (t2 * t2) - (t2 * t2 * t2);

        double l8coef = 1385.0 - 3111.0 * t2 + 543.0 * (t2 * t2) - (t2 * t2 * t2);

        return new double[] {
                /* Calculate easting (x) */
                N_a * cos(phi) * l
                + (N_a / 6.0 * pow(cos(phi), 3.0) * l3coef * pow(l, 3.0))
                + (N_a / 120.0 * pow(cos(phi), 5.0) * l5coef * pow(l, 5.0))
                + (N_a / 5040.0 * pow(cos(phi), 7.0) * l7coef * pow(l, 7.0)),
                /* Calculate northing (y) */
                ArcLengthOfMeridian (phi) / a
                + (t / 2.0 * N_a * pow(cos(phi), 2.0) * pow(l, 2.0))
                + (t / 24.0 * N_a * pow(cos(phi), 4.0) * l4coef * pow(l, 4.0))
                + (t / 720.0 * N_a * pow(cos(phi), 6.0) * l6coef * pow(l, 6.0))
                + (t / 40320.0 * N_a * pow(cos(phi), 8.0) * l8coef * pow(l, 8.0)) };
    }

    /**
     * Converts x and y coordinates in the Transverse Mercator projection to
     * a latitude/longitude pair.  Note that Transverse Mercator is not
     * the same as UTM; a scale factor is required to convert between them.
     *
     * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
     *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
     *
     * Remarks:
     *   The local variables Nf, nuf2, tf, and tf2 serve the same purpose as
     *   N, nu2, t, and t2 in MapLatLonToXY, but they are computed with respect
     *   to the footpoint latitude phif.
     *
     *   x1frac, x2frac, x2poly, x3poly, etc. are to enhance readability and
     *   to optimize computations.
     *
     * @param x The easting of the point, in meters, divided by the semi major axis of the ellipsoid
     * @param y The northing of the point, in meters, divided by the semi major axis of the ellipsoid
     * @return A 2-element containing the latitude and longitude
     *               in radians
     */
    @Override
    public double[] invproject(double x, double y) {
        /* Get the value of phif, the footpoint latitude. */
        double phif = footpointLatitude(y);

        /* Precalculate ep2 */
        double ep2 = (a*a - b*b)
        / (b*b);

        /* Precalculate cos(phif) */
        double cf = cos(phif);

        /* Precalculate nuf2 */
        double nuf2 = ep2 * pow(cf, 2.0);

        /* Precalculate Nf / a and initialize Nfpow */
        double Nf_a = a / (b * sqrt(1 + nuf2));
        double Nfpow = Nf_a;

        /* Precalculate tf */
        double tf = tan(phif);
        double tf2 = tf * tf;
        double tf4 = tf2 * tf2;

        /* Precalculate fractional coefficients for x**n in the equations
           below to simplify the expressions for latitude and longitude. */
        double x1frac = 1.0 / (Nfpow * cf);

        Nfpow *= Nf_a;   /* now equals Nf**2) */
        double x2frac = tf / (2.0 * Nfpow);

        Nfpow *= Nf_a;   /* now equals Nf**3) */
        double x3frac = 1.0 / (6.0 * Nfpow * cf);

        Nfpow *= Nf_a;   /* now equals Nf**4) */
        double x4frac = tf / (24.0 * Nfpow);

        Nfpow *= Nf_a;   /* now equals Nf**5) */
        double x5frac = 1.0 / (120.0 * Nfpow * cf);

        Nfpow *= Nf_a;   /* now equals Nf**6) */
        double x6frac = tf / (720.0 * Nfpow);

        Nfpow *= Nf_a;   /* now equals Nf**7) */
        double x7frac = 1.0 / (5040.0 * Nfpow * cf);

        Nfpow *= Nf_a;   /* now equals Nf**8) */
        double x8frac = tf / (40320.0 * Nfpow);

        /* Precalculate polynomial coefficients for x**n.
           -- x**1 does not have a polynomial coefficient. */
        double x2poly = -1.0 - nuf2;
        double x3poly = -1.0 - 2 * tf2 - nuf2;
        double x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * (nuf2 *nuf2) - 9.0 * tf2 * (nuf2 * nuf2);
        double x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
        double x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2 + 162.0 * tf2 * nuf2;
        double x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
        double x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2);

        return new double[] {
                /* Calculate latitude */
                        phif + x2frac * x2poly * (x * x)
                        + x4frac * x4poly * pow(x, 4.0)
                        + x6frac * x6poly * pow(x, 6.0)
                        + x8frac * x8poly * pow(x, 8.0),
                        /* Calculate longitude */
                        x1frac * x
                        + x3frac * x3poly * pow(x, 3.0)
                        + x5frac * x5poly * pow(x, 5.0)
                        + x7frac * x7poly * pow(x, 7.0) };
    }

    /**
     * ArcLengthOfMeridian
     *
     * Computes the ellipsoidal distance from the equator to a point at a
     * given latitude.
     *
     * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
     * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
     *
     * @param phi Latitude of the point, in radians
     * @return The ellipsoidal distance of the point from the equator
     *         (in meters, divided by the semi major axis of the ellipsoid)
     */
    private double ArcLengthOfMeridian(double phi) {
        /* Precalculate n */
        double n = (a - b) / (a + b);

        /* Precalculate alpha */
        double alpha = ((a + b) / 2.0)
            * (1.0 + (pow(n, 2.0) / 4.0) + (pow(n, 4.0) / 64.0));

        /* Precalculate beta */
        double beta = (-3.0 * n / 2.0) + (9.0 * pow(n, 3.0) / 16.0)
            + (-3.0 * pow(n, 5.0) / 32.0);

        /* Precalculate gamma */
        double gamma = (15.0 * pow(n, 2.0) / 16.0)
            + (-15.0 * pow(n, 4.0) / 32.0);

        /* Precalculate delta */
        double delta = (-35.0 * pow(n, 3.0) / 48.0)
            + (105.0 * pow(n, 5.0) / 256.0);

        /* Precalculate epsilon */
        double epsilon = (315.0 * pow(n, 4.0) / 512.0);

        /* Now calculate the sum of the series and return */
        return alpha
            * (phi + (beta * sin(2.0 * phi))
                    + (gamma * sin(4.0 * phi))
                    + (delta * sin(6.0 * phi))
                    + (epsilon * sin(8.0 * phi)));
    }

    /**
     * FootpointLatitude
     *
     * Computes the footpoint latitude for use in converting transverse
     * Mercator coordinates to ellipsoidal coordinates.
     *
     * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
     *   GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
     *
     * @param y northing coordinate, in meters, divided by the semi major axis of the ellipsoid
     * @return The footpoint latitude, in radians
     */
    private double footpointLatitude(double y) {
        /* Precalculate n (Eq. 10.18) */
        double n = (a - b) / (a + b);

        /* Precalculate alpha_ (Eq. 10.22) */
        /* (Same as alpha in Eq. 10.17) */
        double alpha_ = ((a + b) / 2.0)
            * (1 + (pow(n, 2.0) / 4) + (pow(n, 4.0) / 64));

        /* Precalculate y_ (Eq. 10.23) */
        double y_ = y / alpha_ * a;

        /* Precalculate beta_ (Eq. 10.22) */
        double beta_ = (3.0 * n / 2.0) + (-27.0 * pow(n, 3.0) / 32.0)
            + (269.0 * pow(n, 5.0) / 512.0);

        /* Precalculate gamma_ (Eq. 10.22) */
        double gamma_ = (21.0 * pow(n, 2.0) / 16.0)
            + (-55.0 * pow(n, 4.0) / 32.0);

        /* Precalculate delta_ (Eq. 10.22) */
        double delta_ = (151.0 * pow(n, 3.0) / 96.0)
            + (-417.0 * pow(n, 5.0) / 128.0);

        /* Precalculate epsilon_ (Eq. 10.22) */
        double epsilon_ = (1097.0 * pow(n, 4.0) / 512.0);

        /* Now calculate the sum of the series (Eq. 10.21) */
        return y_ + (beta_ * sin(2.0 * y_))
            + (gamma_ * sin(4.0 * y_))
            + (delta_ * sin(6.0 * y_))
            + (epsilon_ * sin(8.0 * y_));
    }

}
