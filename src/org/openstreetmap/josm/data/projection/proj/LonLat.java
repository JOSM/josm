// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

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
        CheckParameterUtil.ensureParameterNotNull(params, "params");
        CheckParameterUtil.ensureParameterNotNull(params.ellps, "params.ellps");
        a = params.ellps.a;
    }

    @Override
    public double[] project(double latRad, double lonRad) {
        return new double[] {Utils.toDegrees(lonRad) / a, Utils.toDegrees(latRad) / a};
    }

    @Override
    public double[] invproject(double east, double north) {
        return new double[] {Utils.toRadians(north * a), Utils.toRadians(east * a)};
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-90, -180, 90, 180, false);
    }

    @Override
    public boolean isGeographic() {
        return true;
    }
}
