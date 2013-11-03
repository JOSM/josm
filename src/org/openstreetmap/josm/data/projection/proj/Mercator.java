// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.log;
import static java.lang.Math.sinh;
import static java.lang.Math.tan;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Mercator Projection.
 */
public class Mercator implements Proj {

    @Override
    public String getName() {
        return tr("Mercator");
    }

    @Override
    public String getProj4Id() {
        return "josm:smerc"; // "merc" is ellipsoidal Mercator projection in PROJ.4
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
    }

    @Override
    public double[] project(double lat_rad, double lon_rad) {
        return new double[] { lon_rad, log(tan(PI/4 + lat_rad/2)) };
    }

    @Override
    public double[] invproject(double east, double north) {
        return new double[] { atan(sinh(north)), east };
    }

}
