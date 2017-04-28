// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * The polar case of the stereographic projection.
 * <p>
 * In the proj.4 library, the code "stere" covers several variants of the
 * Stereographic projection, depending on the latitude of natural origin
 * (parameter lat_0).
 * <p>
 *
 * In this file, only the polar case is implemented. This corresponds to
 * EPSG:9810 (Polar Stereographic Variant A) and EPSG:9829 (Polar Stereographic
 * Variant B).
 * <p>
 *
 * It is required, that the latitude of natural origin has the value +/-90 degrees.
 * <p>
 *
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.PolarStereographic
 * at the time of migration.
 * <p>
 *
 * <b>References:</b>
 * <ul>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>
 *       EPSG Guidence Note Number 7, Version 19.</li>
 *   <li>Gerald Evenden. <A HREF="http://members.bellatlantic.net/~vze2hc4d/proj4/sterea.pdf">
 *       "Supplementary PROJ.4 Notes - Oblique Stereographic Alternative"</A></li>
 *   <li>Krakiwsky, E.J., D.B. Thomson, and R.R. Steeves. 1977. A Manual
 *       For Geodetic Coordinate Transformations in the Maritimes.
 *       Geodesy and Geomatics Engineering, UNB. Technical Report No. 48.</li>
 *   <li>Thomson, D.B., M.P. Mepham and R.R. Steeves. 1977.
 *       The Stereographic Double Projection.
 *       Geodesy and Geomatics Engineereng, UNB. Technical Report No. 46.</li>
 * </ul>
 *
 * @author André Gosselin
 * @author Martin Desruisseaux (PMO, IRD)
 * @author Rueben Schulz
 *
 * @see <A HREF="http://mathworld.wolfram.com/StereographicProjection.html">Stereographic projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/polar_stereographic.html">Polar_Stereographic</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/oblique_stereographic.html">Oblique_Stereographic</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/stereographic.html">Stereographic</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/random_issues.html#stereographic">Some Random Stereographic Issues</A>
 *
 * @see DoubleStereographic
 * @since 9419
 */
public class PolarStereographic extends AbstractProj {
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
    private static final double EPSILON = 1E-8;

    /**
     * A constant used in the transformations.
     */
    private double k0;

    /**
     * {@code true} if this projection is for the south pole, or {@code false}
     * if it is for the north pole.
     */
    boolean southPole;

    @Override
    public String getName() {
        return tr("Polar Stereographic");
    }

    @Override
    public String getProj4Id() {
        return "stere";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        if (params.lat0 != 90.0 && params.lat0 != -90.0)
            throw new ProjectionConfigurationException(
                    tr("Polar Stereographic: Parameter ''{0}'' must be 90 or -90.", "lat_0"));
        // Latitude of true scale, in radians;
        double latitudeTrueScale;
        if (params.lat_ts == null) {
            latitudeTrueScale = (params.lat0 < 0) ? -Math.PI/2 : Math.PI/2;
        } else {
            latitudeTrueScale = Utils.toRadians(params.lat_ts);
        }
        southPole = latitudeTrueScale < 0;

        // Computes coefficients.
        double latitudeTrueScaleAbs = Math.abs(latitudeTrueScale);
        if (Math.abs(latitudeTrueScaleAbs - Math.PI/2) >= EPSILON) {
            final double t = Math.sin(latitudeTrueScaleAbs);
            k0 = msfn(t, Math.cos(latitudeTrueScaleAbs)) /
                 tsfn(latitudeTrueScaleAbs, t); // Derives from (21-32 and 21-33)
        } else {
            // True scale at pole (part of (21-33))
            k0 = 2.0 / Math.sqrt(Math.pow(1+e, 1+e)*
                                 Math.pow(1-e, 1-e));
        }
    }

    @Override
    public double[] project(double y, double x) {
        final double sinlat = Math.sin(y);
        final double coslon = Math.cos(x);
        final double sinlon = Math.sin(x);
        if (southPole) {
            final double rho = k0 * tsfn(-y, -sinlat);
            x = rho * sinlon;
            y = rho * coslon;
        } else {
            final double rho = k0 * tsfn(y, sinlat);
            x = rho * sinlon;
            y = -rho * coslon;
        }
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        final double rho = Math.hypot(x, y);
        if (southPole) {
            y = -y;
        }
        /*
         * Compute latitude using iterative technique.
         */
        final double t = rho/k0;
        final double halfe = e/2.0;
        double phi0 = 0;
        for (int i = MAXIMUM_ITERATIONS;;) {
            final double esinphi = e * Math.sin(phi0);
            final double phi = (Math.PI/2) - 2.0*Math.atan(t*Math.pow((1-esinphi)/(1+esinphi), halfe));
            if (Math.abs(phi-phi0) < ITERATION_TOLERANCE) {
                x = (Math.abs(rho) < EPSILON) ? 0.0 : Math.atan2(x, -y);
                y = southPole ? -phi : phi;
                break;
            }
            phi0 = phi;
            if (--i < 0) {
                throw new IllegalStateException("no convergence for x="+x+", y="+y);
            }
        }
        return new double[] {y, x};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        final double cut = 60;
        if (southPole) {
            return new Bounds(-90, -180, cut, 180, false);
        } else {
            return new Bounds(-cut, -180, 90, 180, false);
        }
    }
}
