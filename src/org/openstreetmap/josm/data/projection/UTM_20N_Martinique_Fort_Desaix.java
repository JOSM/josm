// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

/*
 * Local geodisic system with UTM zone 20N projection.
 * Apply to Martinique, France and surrounding islands
 */
public class UTM_20N_Martinique_Fort_Desaix extends UTM_20N_France_DOM implements Projection {
    public UTM_20N_Martinique_Fort_Desaix() {
        super(new double[]{126.926, 547.939, 130.409},
                new double[]{-2.78670, 5.16124,  -0.85844},
                13.82265E-6);
    }

    public String getCacheDirectoryName() {
        return this.toString();
    }

    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(14.25,-61.25),
                new LatLon(15.025,-60.725));
    }

    public String toCode() {
        return "EPSG::2973";
    }

    @Override public String toString() {
        return tr("UTM20N Martinique Fort Desaix 1952");
    }
}
