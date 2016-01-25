// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
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
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Implementation of the stereographic double projection,
 * also known as Oblique Stereographic and the Schreiber double stereographic projection.
 *
 * @author vholten
 *
 * Source: IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2,
 * Sec. 1.3.7.1 Oblique and Equatorial Stereographic, http://www.epsg.org/GuidanceNotes
 */
public class DoubleStereographic extends AbstractProj {

    private Ellipsoid ellps;
    private double n;
    private double c;
    private double chi0;
    private double R;

    private static final double EPSILON = 1e-12;

    @Override
    public String getName() {
        return tr("Double Stereographic");
    }

    @Override
    public String getProj4Id() {
        return "sterea";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        ellps = params.ellps;
        initialize(params.lat0);
    }

    private void initialize(double lat_0) {
        double phi0 = toRadians(lat_0);
        double e2 = ellps.e2;
        R = sqrt(1-e2) / (1 - e2*pow(sin(phi0), 2));
        n = sqrt(1 + ellps.eb2 * pow(cos(phi0), 4));
        double S1 = (1 + sin(phi0)) / (1 - sin(phi0));
        double S2 = (1 - e * sin(phi0)) / (1 + e * sin(phi0));
        double w1 = pow(S1 * pow(S2, e), n);
        double sinchi00 = (w1 - 1) / (w1 + 1);
        c = (n + sin(phi0)) * (1 - sinchi00) / ((n - sin(phi0)) * (1 + sinchi00));
        double w2 = c * w1;
        chi0 = asin((w2 - 1) / (w2 + 1));
    }

    @Override
    public double[] project(double phi, double lambda) {
        double Lambda = n * lambda;
        double Sa = (1 + sin(phi)) / (1 - sin(phi));
        double Sb = (1 - e * sin(phi)) / (1 + e * sin(phi));
        double w = c * pow(Sa * pow(Sb, e), n);
        double chi = asin((w - 1) / (w + 1));
        double B = 1 + sin(chi) * sin(chi0) + cos(chi) * cos(chi0) * cos(Lambda);
        double x = 2 * R * cos(chi) * sin(Lambda) / B;
        double y = 2 * R * (sin(chi) * cos(chi0) - cos(chi) * sin(chi0) * cos(Lambda)) / B;
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        double e2 = ellps.e2;
        double g = 2 * R * tan(PI/4 - chi0/2);
        double h = 4 * R * tan(chi0) + g;
        double i = atan(x/(h + y));
        double j = atan(x/(g - y)) - i;
        double chi = chi0 + 2 * atan((y - x * tan(j/2)) / (2 * R));
        double Lambda = j + 2*i;
        double lambda = Lambda / n;
        double psi = 0.5 * log((1 + sin(chi)) / (c*(1 - sin(chi)))) / n;
        double phiprev = -1000;
        int iteration = 0;
        double phi = 2 * atan(exp(psi)) - PI/2;
        while (abs(phi - phiprev) > EPSILON) {
            if (++iteration > 10)
                throw new RuntimeException("Too many iterations");
            phiprev = phi;
            double psii = log(tan(phi/2 + PI/4) * pow((1 - e * sin(phi)) / (1 + e * sin(phi)), e/2));
            phi = phi - (psii - psi) * cos(phi) * (1 - e2 * pow(sin(phi), 2)) / (1 - e2);
        }
        return new double[] {phi, lambda};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -87, 89, 87, false);
    }
}
