// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * A datum with different ellipsoid than WGS84, but does not require
 * shift, rotation or scaling.
 * @since 4285
 */
public class CentricDatum extends AbstractDatum {

    /**
     * Constructs a new {@code CentricDatum}.
     * @param name Datum name
     * @param proj4Id proj.4 identifier
     * @param ellps Ellipsoid. Must be non-null and different from WGS84
     */
    public CentricDatum(String name, String proj4Id, Ellipsoid ellps) {
        super(name, proj4Id, ellps);
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        return Ellipsoid.WGS84.cart2LatLon(ellps.latLon2Cart(ll));
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        return this.ellps.cart2LatLon(Ellipsoid.WGS84.latLon2Cart(ll));
    }

    @Override
    public String toString() {
        return "CentricDatum{ellipsoid="+ellps+'}';
    }
}
