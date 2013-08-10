//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
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
 * Projection for the SwissGrid CH1903 / L03, see http://en.wikipedia.org/wiki/Swiss_coordinate_system.
 *
 * Calculations were originally based on simple formula from
 * http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/switzerland.parsysrelated1.37696.downloadList.12749.DownloadFile.tmp/ch1903wgs84en.pdf
 *
 * August 2010 update to this formula (rigorous formulas)
 * http://www.swisstopo.admin.ch/internet/swisstopo/en/home/topics/survey/sys/refsys/switzerland.parsysrelated1.37696.downloadList.97912.DownloadFile.tmp/swissprojectionen.pdf
 */
public class SwissObliqueMercator implements Proj {

    private Ellipsoid ellps;
    private double kR;
    private double alpha;
    private double b0;
    private double K;

    private static final double EPSILON = 1e-11;

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        if (params.lat_0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        ellps = params.ellps;
        initialize(params.lat_0);
    }

    private void initialize(double lat_0) {
        double phi0 = toRadians(lat_0);
        kR = sqrt(1 - ellps.e2) / (1 - (ellps.e2 * pow(sin(phi0), 2)));
        alpha = sqrt(1 + (ellps.eb2 * pow(cos(phi0), 4)));
        b0 = asin(sin(phi0) / alpha);
        K = log(tan(PI / 4 + b0 / 2)) - alpha
            * log(tan(PI / 4 + phi0 / 2)) + alpha * ellps.e / 2
            * log((1 + ellps.e * sin(phi0)) / (1 - ellps.e * sin(phi0)));
    }

    @Override
    public String getName() {
        return tr("Swiss Oblique Mercator");
    }

    @Override
    public String getProj4Id() {
        return "somerc";
    }

    @Override
    public double[] project(double phi, double lambda) {

        double S = alpha * log(tan(PI / 4 + phi / 2)) - alpha * ellps.e / 2
            * log((1 + ellps.e * sin(phi)) / (1 - ellps.e * sin(phi))) + K;
        double b = 2 * (atan(exp(S)) - PI / 4);
        double l = alpha * lambda;

        double lb = atan2(sin(l), sin(b0) * tan(b) + cos(b0) * cos(l));
        double bb = asin(cos(b0) * sin(b) - sin(b0) * cos(b) * cos(l));

        double y = kR * lb;
        double x = kR / 2 * log((1 + sin(bb)) / (1 - sin(bb)));

        return new double[] { y, x };
    }

    @Override
    public double[] invproject(double y, double x) {
        double lb = y / kR;
        double bb = 2 * (atan(exp(x / kR)) - PI / 4);

        double b = asin(cos(b0) * sin(bb) + sin(b0) * cos(bb) * cos(lb));
        double l = atan2(sin(lb), cos(b0) * cos(lb) - sin(b0) * tan(bb));

        double lambda = l / alpha;
        double phi = b;
        double S = 0;

        double prevPhi = -1000;
        int iteration = 0;
        // iteration to finds S and phi
        while (abs(phi - prevPhi) > EPSILON) {
            if (++iteration > 30)
                throw new RuntimeException("Two many iterations");
            prevPhi = phi;
            S = 1 / alpha * (log(tan(PI / 4 + b / 2)) - K) + ellps.e
            * log(tan(PI / 4 + asin(ellps.e * sin(phi)) / 2));
            phi = 2 * atan(exp(S)) - PI / 2;
        }
        return new double[] { phi, lambda };
    }

}
