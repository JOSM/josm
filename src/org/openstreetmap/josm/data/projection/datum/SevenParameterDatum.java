// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Datum provides general conversion from one ellipsoid to another.
 *
 * Seven parameters can be specified:
 * - 3D offset
 * - general rotation
 * - scale
 *
 * This method is described by EPSG as EPSG::9606.
 * Also known as Bursa-Wolf.
 */
public class SevenParameterDatum extends AbstractDatum {

    protected double dx, dy, dz, rx, ry, rz, s;

    /**
     *
     * @param name name of the datum
     * @param proj4Id Proj.4 identifier for this datum (or null)
     * @param ellps the ellipsoid used
     * @param dx x offset in meters
     * @param dy y offset in meters
     * @param dz z offset in meters
     * @param rx rotational parameter in seconds of arc
     * @param ry rotational parameter in seconds of arc
     * @param rz rotational parameter in seconds of arc
     * @param s scale change in parts per million
     */
    public SevenParameterDatum(String name, String proj4Id, Ellipsoid ellps, double dx, double dy, double dz, double rx, double ry, double rz, double s) {
        super(name, proj4Id, ellps);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = Math.toRadians(rx / 3600);
        this.ry = Math.toRadians(ry / 3600);
        this.rz = Math.toRadians(rz / 3600);
        this.s = s / 1e6;
    }

    @Override
    public LatLon toWGS84(LatLon ll) {
        double[] xyz = ellps.latLon2Cart(ll);
        double x = dx + xyz[0]*(1+s) + xyz[2]*ry - xyz[1]*rz;
        double y = dy + xyz[1]*(1+s) + xyz[0]*rz - xyz[2]*rx;
        double z = dz + xyz[2]*(1+s) + xyz[1]*rx - xyz[0]*ry;
        return Ellipsoid.WGS84.cart2LatLon(new double[] { x, y, z });
    }

    @Override
    public LatLon fromWGS84(LatLon ll) {
        double[] xyz = Ellipsoid.WGS84.latLon2Cart(ll);
        double x = (1-s)*(-dx + xyz[0] + ((-dz+xyz[2])*(-ry) - (-dy+xyz[1])*(-rz)));
        double y = (1-s)*(-dy + xyz[1] + ((-dx+xyz[0])*(-rz) - (-dz+xyz[2])*(-rx)));
        double z = (1-s)*(-dz + xyz[2] + ((-dy+xyz[1])*(-rx) - (-dx+xyz[0])*(-ry)));
        return this.ellps.cart2LatLon(new double[] { x, y, z });
    }

}
