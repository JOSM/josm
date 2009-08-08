// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

/*
 * Local geodisic system with UTM zone 20N projection.
 * Apply to Guadeloupe, France - Grande-Terre and surrounding islands.
 */
public class UTM_20N_Guadeloupe_Ste_Anne extends UTM_20N_France_DOM implements Projection {
    public UTM_20N_Guadeloupe_Ste_Anne() {
        super (new double[]{-472.29, -5.63, -304.12},
                new double[]{0.4362, -0.8374, 0.2563},
                1.8984E-6);
    }

    public String getCacheDirectoryName() {
        return this.toString();
    }

    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(15.8,-61.8),
                new LatLon(16.6,-60.9));
    }

    public String toCode() {
        return "EPSG::2970";
    }

    @Override public String toString() {
        return tr("UTM20N Guadeloupe Ste-Anne 1948");
    }

}
