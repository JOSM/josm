// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Cassini-Soldner Projection (EPSG code 9806).
 * The Cassini-Soldner Projection is the ellipsoidal version of the Cassini
 * projection for the sphere. It is not conformal but as it is relatively simple
 * to construct it was extensively used in the last century and is still useful
 * for mapping areas with limited longitudinal extent. It has now largely
 * been replaced by the conformal Transverse Mercator which it resembles. Like this,
 * it has a straight central meridian along which the scale is true, all other
 * meridians and parallels are curved, and the scale distortion increases
 * rapidly with increasing distance from the central meridian.
 * <p>
 *
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.CassiniSoldner
 * at the time of migration.
 */
public class CassiniSoldner extends AbstractProj {

    /**
     * Meridian distance at the {@code latitudeOfOrigin}.
     * Used for calculations for the ellipsoid.
     */
    private double ml0;

    /**
     * Latitude of origin.
     */
    private double phi0;

    /**
     * Constants used for the forward and inverse transform for the elliptical
     * case of the Cassini-Soldner.
     */
    private static final double C1 = 1. / 6;
    private static final double C2 = 1. / 120;
    private static final double C3 = 1. / 24;
    private static final double C4 = 1. / 3;
    private static final double C5 = 1. / 15;

    @Override
    public String getName() {
        return tr("Cassini-Soldner");
    }

    @Override
    public String getProj4Id() {
        return "cass";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);
        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));
        phi0 = Utils.toRadians(params.lat0);
        ml0 = mlfn(phi0, Math.sin(phi0), Math.cos(phi0));
    }

    @Override
    public double[] project(double phi, double lam) {
        if (spherical) {
            double x = aasin(Math.cos(phi) * Math.sin(lam));
            double y = Math.atan2(Math.tan(phi), Math.cos(lam));
            return new double[] {x, y};
        } else {
            double sinphi = Math.sin(phi);
            double cosphi = Math.cos(phi);

            double n = 1.0 / Math.sqrt(1.0 - e2 * sinphi * sinphi);
            double tn = Math.tan(phi);
            double t = tn * tn;
            double a1 = lam * cosphi;
            double c = cosphi * cosphi * e2 / (1 - e2);
            double a2 = a1 * a1;

            double x = n * a1 * (1.0 - a2 * t * (C1 - (8.0 - t + 8.0 * c) * a2 * C2));
            double y = mlfn(phi, sinphi, cosphi) - ml0 + n * tn * a2 * (0.5 + (5.0 - t + 6.0 * c) * a2 * C3);
            return new double[] {x, y};
        }
    }

    @Override
    public double[] invproject(double x, double y) {
        if (spherical) {
            double dd = y + phi0;
            double phi = aasin(Math.sin(dd * Math.cos(x)));
            double lam = Math.atan2(Math.tan(x), Math.cos(dd));
            return new double[] {phi, lam};
        } else {
            double ph1 = invMlfn(ml0 + y);
            double tn = Math.tan(ph1);
            double t = tn * tn;
            double n = Math.sin(ph1);
            double r = 1.0 / (1.0 - e2 * n * n);
            n = Math.sqrt(r);
            r *= (1.0 - e2) * n;
            double dd = x / n;
            double d2 = dd * dd;
            double phi = ph1 - (n * tn / r) * d2 * (0.5 - (1.0 + 3.0 * t) * d2 * C3);
            double lam = dd * (1.0 + t * d2 * (-C4 + (1.0 + 3.0 * t) * d2 * C5)) / Math.cos(ph1);
            return new double[] {phi, lam};
        }
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -1.0, 89, 1.0, false);
    }
}
