// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Represents a geodetic datum.
 *
 * Basically it provides conversion functions from and to the WGS84 datum.
 * @since 4285
 */
public interface Datum {

    /**
     * @return a human readable name of this projection
     */
    String getName();

    /**
     * Replies the Proj.4 identifier.
     * @return the Proj.4 identifier (as reported by cs2cs -ld)
     * If no id exists, return null.
     */
    String getProj4Id();

    /**
     * @return the ellipsoid associated with this datum
     */
    Ellipsoid getEllipsoid();

    /**
     * Convert lat/lon from this datum to {@link Ellipsoid#WGS84} datum.
     * @param ll original lat/lon in this datum
     * @return lat/lon converted to WGS84
     */
    LatLon toWGS84(LatLon ll);

    /**
     * Convert lat/lon from {@link Ellipsoid#WGS84} to this datum.
     * @param ll original lat/lon in WGS84
     * @return converted lat/lon in this datum
     */
    LatLon fromWGS84(LatLon ll);
}
