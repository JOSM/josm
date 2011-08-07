// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * WGS84 datum. Transformation from and to WGS84 datum is a no-op.
 */
public class WGS84Datum extends AbstractDatum {

    public static WGS84Datum INSTANCE = new WGS84Datum();

    private WGS84Datum() {
        super(tr("WGS84"), "WGS84", Ellipsoid.WGS84);
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        return ll;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        return ll;
    }
}
