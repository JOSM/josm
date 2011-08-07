// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static java.lang.Math.*;

import static org.openstreetmap.josm.tools.I18n.tr;

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
        return "merc";
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
