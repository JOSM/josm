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

import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Implementation of the Lambert Conformal Conic projection.
 *
 * @author Pieren
 */
public class LambertConformalConic implements Proj {

    protected Ellipsoid ellps;
    protected double e;

    public static abstract class Parameters {
        public final double latitudeOrigin;
        public Parameters(double latitudeOrigin) {
            this.latitudeOrigin = latitudeOrigin;
        }
    }

    public static class Parameters1SP extends Parameters {
        public Parameters1SP(double latitudeOrigin) {
            super(latitudeOrigin);
        }
    }

    public static class Parameters2SP extends Parameters {
        public final double standardParallel1;
        public final double standardParallel2;
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
    protected double F;
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
        ellps = params.ellps;
        e = ellps.e;
        if (params.lat_0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        if (params.lat_1 != null && params.lat_2 != null) {
            initialize2SP(params.lat_0, params.lat_1, params.lat_2);
        } else {
            initialize1SP(params.lat_0);
        }
    }

    /**
     * Initialize for LCC with 2 standard parallels.
     *
     * @param lat_0 latitude of false origin (in degrees)
     * @param lat_1 latitude of first standard parallel (in degrees)
     * @param lat_2 latitude of second standard parallel (in degrees)
     */
    private void initialize2SP(double lat_0, double lat_1, double lat_2) {
        this.params = new Parameters2SP(lat_0, lat_1, lat_2);

        final double m1 = m(toRadians(lat_1));
        final double m2 = m(toRadians(lat_2));

        final double t1 = t(toRadians(lat_1));
        final double t2 = t(toRadians(lat_2));
        final double tf = t(toRadians(lat_0));

        n  = (log(m1) - log(m2)) / (log(t1) - log(t2));
        F  = m1 / (n * pow(t1, n));
        r0 = F * pow(tf, n);
    }

    /**
     * Initialize for LCC with 1 standard parallel.
     *
     * @param lat_0 latitude of natural origin (in degrees)
     */
    private void initialize1SP(double lat_0) {
        this.params = new Parameters1SP(lat_0);
        final double lat_0_rad = toRadians(lat_0);

        final double m0 = m(lat_0_rad);
        final double t0 = t(lat_0_rad);

        n = sin(lat_0_rad);
        F  = m0 / (n * pow(t0, n));
        r0 = F * pow(t0, n);
    }

    /**
     * auxiliary function t
     */
    protected double t(double lat_rad) {
        return tan(PI/4 - lat_rad / 2.0)
            / pow(( (1.0 - e * sin(lat_rad)) / (1.0 + e * sin(lat_rad))) , e/2);
    }

    /**
     * auxiliary function m
     */
    protected double m(double lat_rad) {
        return cos(lat_rad) / (sqrt(1 - e * e * pow(sin(lat_rad), 2)));
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
        double L = (0.5*log((1+sinphi)/(1-sinphi))) - e/2*log((1+e*sinphi)/(1-e*sinphi));
        double r = F*exp(-n*L);
        double gamma = n*lambda;
        double X = r*sin(gamma);
        double Y = r0 - r*cos(gamma);
        return new double[] { X, Y };
    }

    @Override
    public double[] invproject(double east, double north) {
        double r = sqrt(pow(east,2) + pow(north-r0, 2));
        double gamma = atan(east / (r0-north));
        double lambda = gamma/n;
        double latIso = (-1/n) * log(abs(r/F));
        double phi = ellps.latitude(latIso, e, epsilon);
        return new double[] { phi, lambda };
    }

    public final Parameters getParameters() {
        return params;
    }
}
