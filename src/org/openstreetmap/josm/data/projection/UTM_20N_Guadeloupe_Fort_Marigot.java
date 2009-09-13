// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

/*
 * Local geodisic system with UTM zone 20N projection.
 * Apply to Guadeloupe, France - St Martin and St Barthelemy islands
 */
public class UTM_20N_Guadeloupe_Fort_Marigot extends UTM_20N_France_DOM implements Projection {
    public UTM_20N_Guadeloupe_Fort_Marigot() {
        super(new double[]{136.596, 248.148, -429.789},
                new double[]{0, 0, 0},
                0);
    }

    public String getCacheDirectoryName() {
        return this.toString();
    }

    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(17.6,-63.25),
                new LatLon(18.5,-62.5));
    }

    public String toCode() {
        return "EPSG::2969";
    }

    @Override public String toString() {
        return tr("UTM20N Guadeloupe Fort-Marigot 1949");
    }

    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m
        return 10;
    }
}
