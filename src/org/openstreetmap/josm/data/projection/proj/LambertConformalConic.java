// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.CustomProjection.Param;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Implementation of the Lambert Conformal Conic projection.
 *
 * @author Pieren
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
     * radius of the parallel of latitude of the false origin (2SP) or at
     * natural origin (1SP)
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
            initialize2SP(params.lat0, params.lat1, params.lat2);
        } else {
            initialize1SP(params.lat0);
        }
    }

    /**
     * Initialize for LCC with 2 standard parallels.
     *
     * @param lat0 latitude of false origin (in degrees)
     * @param lat1 latitude of first standard parallel (in degrees)
     * @param lat2 latitude of second standard parallel (in degrees)
     */
    private void initialize2SP(double lat0, double lat1, double lat2) {
        this.params = new Parameters2SP(lat0, lat1, lat2);

        final double m1 = m(toRadians(lat1));
        final double m2 = m(toRadians(lat2));

        final double t1 = t(toRadians(lat1));
        final double t2 = t(toRadians(lat2));
        final double tf = t(toRadians(lat0));

        n = (log(m1) - log(m2)) / (log(t1) - log(t2));
        f = m1 / (n * pow(t1, n));
        r0 = f * pow(tf, n);
    }

    /**
     * Initialize for LCC with 1 standard parallel.
     *
     * @param lat0 latitude of natural origin (in degrees)
     */
    private void initialize1SP(double lat0) {
        this.params = new Parameters1SP(lat0);
        final double lat0rad = toRadians(lat0);

        final double m0 = m(lat0rad);
        final double t0 = t(lat0rad);

        n = sin(lat0rad);
        f = m0 / (n * pow(t0, n));
        r0 = f * pow(t0, n);
    }

    /**
     * auxiliary function t
     * @param latRad latitude in radians
     * @return result
     */
    protected double t(double latRad) {
        return tan(PI/4 - latRad / 2.0)
            / pow((1.0 - e * sin(latRad)) / (1.0 + e * sin(latRad)), e/2);
    }

    /**
     * auxiliary function m
     * @param latRad latitude in radians
     * @return result
     */
    protected double m(double latRad) {
        return cos(latRad) / (sqrt(1 - e * e * pow(sin(latRad), 2)));
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
