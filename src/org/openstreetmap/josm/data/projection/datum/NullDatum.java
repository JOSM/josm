// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Null Datum does not convert from / to WGS84 ellipsoid, but simply "casts"
 * the coordinates.
 */
public class NullDatum extends AbstractDatum {

    public NullDatum(String name, Ellipsoid ellps) {
        super(name, null, ellps);
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        return ll;
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        return ll;
    }

}
