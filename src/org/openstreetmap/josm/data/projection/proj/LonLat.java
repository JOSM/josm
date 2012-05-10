// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Simple Lat/Lon (pseudo-)projection.
 */
public class LonLat implements Proj {

    private double a;

    @Override
    public String getName() {
        return tr("Lat/lon (Geodetic)");
    }

    @Override
    public String getProj4Id() {
        return "lonlat";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        a = params.ellps.a;
    }

    @Override
    public double[] project(double lat_rad, double lon_rad) {
        return new double[] { Math.toDegrees(lon_rad) / a, Math.toDegrees(lat_rad) / a };
    }

    @Override
    public double[] invproject(double east, double north) {
        return new double[] { Math.toRadians(north * a), Math.toRadians(east * a) };
    }
}
