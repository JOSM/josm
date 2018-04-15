// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.toRadians;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.CustomProjection.Param;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Lambert Conical Conformal Projection. Areas and shapes are deformed as one moves away from standard parallels.
 * The angles are true in a limited area. This projection is used for the charts of North America, France and Belgium.
 * <p>
 * This implementation provides transforms for two cases of the lambert conic conformal projection:
 * <p>
 * <ul>
 *   <li>{@code Lambert_Conformal_Conic_1SP} (EPSG code 9801)</li>
 *   <li>{@code Lambert_Conformal_Conic_2SP} (EPSG code 9802)</li>
 * </ul>
 * <p>
 * For the 1SP case the latitude of origin is used as the standard parallel (SP).
 * To use 1SP with a latitude of origin different from the SP, use the 2SP and set the SP1 to the single SP.
 * The {@code "standard_parallel_2"} parameter is optional and will be given the same value
 * as {@code "standard_parallel_1"} if not set (creating a 1 standard parallel projection).
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>U.S. Geological Survey Professional Paper 1395, 1987)</li>
 *   <li>"Coordinate Conversions and Transformations including Formulas",<br>EPSG Guidence Note Number 7, Version 19.</li>
 * </ul>
 *
 * @author Pieren
 * @author André Gosselin
 * @author Martin Desruisseaux (PMO, IRD)
 * @author Rueben Schulz
 *
 * @see <A HREF="http://mathworld.wolfram.com/LambertConformalConicProjection.html">Lambert conformal conic projection on MathWorld</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/lambert_conic_conformal_1sp.html">lambert_conic_conformal_1sp</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/lambert_conic_conformal_2sp.html">lambert_conic_conformal_2sp</A>
 *
 * @since 13639 (align implementation with proj.4 / GeoTools)
 * @since 4285 (reworked from Lambert / LambertCC9Zones)
 * @since 2304 (initial implementation by Pieren)
 */
public class LambertConformalConic extends AbstractProj {

    /** ellipsoid */
    protected Ellipsoid ellps;

    /**
     * Base class of Lambert Conformal Conic parameters.
     */
    public static class Parameters {
        /** latitude of origin */
        public final double latitudeOrigin;

        /**
         * Constructs a new {@code Parameters}.
         * @param latitudeOrigin latitude of origin
         */
        protected Parameters(double latitudeOrigin) {
            this.latitudeOrigin = latitudeOrigin;
        }
    }

    /**
     * Parameters with a single standard parallel.
     */
    public static class Parameters1SP extends Parameters {
        /**
         * Constructs a new {@code Parameters1SP}.
         * @param latitudeOrigin latitude of origin
         */
        public Parameters1SP(double latitudeOrigin) {
            super(latitudeOrigin);
        }
    }

    /**
     * Parameters with two standard parallels.
     */
    public static class Parameters2SP extends Parameters {
        /** first standard parallel */
        public final double standardParallel1;
        /** second standard parallel */
        public final double standardParallel2;

        /**
         * Constructs a new {@code Parameters2SP}.
         * @param latitudeOrigin latitude of origin
         * @param standardParallel1 first standard parallel
         * @param standardParallel2 second standard parallel
         */
        public Parameters2SP(double latitudeOrigin, double standardParallel1, double standardParallel2) {
            super(latitudeOrigin);
            this.standardParallel1 = standardParallel1;
            this.standardParallel2 = standardParallel2;
        }
    }

    private Parameters params;

    /**
     * projection exponent
     */
    protected double n;

    /**
     * projection factor
     */
    protected double f;

    /**
     * radius of the parallel of latitude of the false origin (2SP) or at natural origin (1SP)
     */
    protected double r0;

    /**
     * precision in iterative schema
     */
    protected static final double epsilon = 1e-12;

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        ellps = params.ellps;
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", Param.lat_0.key));
        if (params.lat1 != null && params.lat2 != null) {
            this.params = new Parameters2SP(params.lat0, params.lat1, params.lat2);
            initialize2SP(toRadians(params.lat0), toRadians(params.lat1), toRadians(params.lat2));
        } else {
            this.params = new Parameters1SP(params.lat0);
            initialize1SP(toRadians(params.lat0));
        }
    }

    /**
     * Initialize for LCC with 2 standard parallels.
     *
     * @param lat0 latitude of false origin (in radians)
     * @param lat1 latitude of first standard parallel (in radians)
     * @param lat2 latitude of second standard parallel (in radians)
     */
    private void initialize2SP(double lat0, double lat1, double lat2) {

        final double cosphi1 = cos(lat1);
        final double sinphi1 = sin(lat1);

        final double cosphi2 = cos(lat2);
        final double sinphi2 = sin(lat2);

        final double m1 = msfn(sinphi1, cosphi1);
        final double m2 = msfn(sinphi2, cosphi2);

        final double t0 = tsfn(lat0, sin(lat0));
        final double t1 = tsfn(lat1, sinphi1);
        final double t2 = tsfn(lat2, sinphi2);

        n = log(m1/m2) / log(t1/t2);
        f = m1 * pow(t1, -n) / n;
        r0 = f * pow(t0, n);
    }

    /**
     * Initialize for LCC with 1 standard parallel.
     *
     * @param lat0 latitude of natural origin (in radians)
     */
    private void initialize1SP(double lat0) {
        final double m0 = msfn(sin(lat0), cos(lat0));
        final double t0 = tsfn(lat0, sin(lat0));

        n = sin(lat0);
        f = m0 * pow(t0, -n) / n;
        r0 = f * pow(t0, n);
    }

    @Override
    public String getName() {
        return tr("Lambert Conformal Conic");
    }

    @Override
    public String getProj4Id() {
        return "lcc";
    }

    @Override
    public double[] project(double phi, double lambda) {
        double sinphi = sin(phi);
        double l = (0.5*log((1+sinphi)/(1-sinphi))) - e/2*log((1+e*sinphi)/(1-e*sinphi));
        double r = f*exp(-n*l);
        double gamma = n*lambda;
        double x = r*sin(gamma);
        double y = r0 - r*cos(gamma);
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double east, double north) {
        double r = sqrt(pow(east, 2) + pow(north-r0, 2));
        double gamma = atan(east / (r0-north));
        double lambda = gamma/n;
        double latIso = (-1/n) * log(abs(r/f));
        double phi = ellps.latitude(latIso, e, epsilon);
        return new double[] {phi, lambda};
    }

    /**
     * Returns projection parameters.
     * @return projection parameters
     */
    public final Parameters getParameters() {
        return params;
    }

    @Override
    public Bounds getAlgorithmBounds() {
        double lat;
        if (params instanceof Parameters2SP) {
            Parameters2SP p2p = (Parameters2SP) params;
            lat = (p2p.standardParallel1 + p2p.standardParallel2) / 2;
        } else {
            lat = params.latitudeOrigin;
        }
        double minlat = Math.max(lat - 60, -89);
        double maxlat = Math.min(lat + 60, 89);
        return new Bounds(minlat, -85, maxlat, 85, false);
    }
}
