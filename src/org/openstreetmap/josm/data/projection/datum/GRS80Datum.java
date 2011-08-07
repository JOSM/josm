// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * This datum indicates, that GRS80 ellipsoid is used and no conversion
 * is necessary to get from or to the WGS84 datum.
 */
public class GRS80Datum extends AbstractDatum {

    public static GRS80Datum INSTANCE = new GRS80Datum();
    
    private GRS80Datum() {
        super(tr("GRS80"), null, Ellipsoid.GRS80);
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
