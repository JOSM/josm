// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Represents a geodetic datum.
 *
 * Basically it provides conversion functions from and to the WGS84 datum.
 */
public interface Datum {

    /**
     * @return a human readable name of this projection
     */
    String getName();

    /**
     * @return the Proj.4 identifier
     * (as reported by cs2cs -ld)
     * If no id exists, return null.
     */
    String getProj4Id();

    /**
     * @return the ellipsoid associated with this datum
     */
    Ellipsoid getEllipsoid();

    /**
     * Convert lat/lon from this datum to WGS84 datum.
     */
    LatLon toWGS84(LatLon ll);

    /**
     * Convert lat/lon from WGS84 to this datum.
     */
    LatLon fromWGS84(LatLon ll);

}
