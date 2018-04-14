// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Datum provides 3 dimensional offset and ellipsoid conversion.
 * @since 4285
 */
public class ThreeParameterDatum extends AbstractDatum {

    protected double dx, dy, dz;

    /**
     * Constructs a new {@code ThreeParameterDatum}.
     * @param name name of the datum
     * @param proj4Id Proj.4 identifier for this datum (or null)
     * @param ellps the ellipsoid used
     * @param dx x offset in meters
     * @param dy y offset in meters
     * @param dz z offset in meters
     */
    public ThreeParameterDatum(String name, String proj4Id, Ellipsoid ellps, double dx, double dy, double dz) {
        super(name, proj4Id, ellps);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        double[] xyz = ellps.latLon2Cart(ll);
        xyz[0] += dx;
        xyz[1] += dy;
        xyz[2] += dz;
        return Ellipsoid.WGS84.cart2LatLon(xyz);
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        double[] xyz = Ellipsoid.WGS84.latLon2Cart(ll);
        xyz[0] -= dx;
        xyz[1] -= dy;
        xyz[2] -= dz;
        return this.ellps.cart2LatLon(xyz);
    }

}
