// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;

/**
 * Sinusoidal projection (aka. Sanson–Flamsteed, Mercator equal-area projection)
 * <p>
 * This class has been derived from the implementation of the <a href="https://github.com/geotools/geotools">Geotools</a> project;
 * git 577dd2d, org.geotools.referencing.operation.projection.Sinusoidal at the time of migration.
 */
public class Sinusoidal extends AbstractProj {

    @Override
    public String getName() {
        return tr("Sinusoidal");
    }

    @Override
    public String getProj4Id() {
        return "sinu";
    }

    @Override
    public double[] project(final double phi, final double lambda) {
        if (spherical) {
            return new double[]{lambda * cos(phi), phi};
        } else {
            final double s = sin(phi);
            return new double[]{lambda * cos(phi) / sqrt(1. - e2 * s * s), mlfn(phi, s, cos(phi))};
        }
    }

    @Override
    public double[] invproject(final double east, final double north) {
        if (spherical) {
            return new double[]{north, east / cos(north)};
        } else {
            final double phi = invMlfn(north);
            double s = abs(phi);
            final double lambda;
            if (abs(s - Math.PI / 2) < 1e-10) {
                lambda = 0.;
            } else if (s < Math.PI / 2) {
                s = sin(phi);
                lambda = (east * sqrt(1. - e2 * s * s) / cos(phi)) % Math.PI;
            } else {
                return new double[]{0., 0.}; // this is an error and should be handled somehow
            }
            return new double[]{phi, lambda};
        }
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-90, -180, 90, 180, false);
    }
}
