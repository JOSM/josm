// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import org.openstreetmap.josm.data.coor.ILatLon;

/**
 * A class that converts lat/lon coordinates to string.
 *
 * @since 12735
 */
public interface ICoordinateFormat {
    /**
     * Get unique id for this coordinate format.
     * @return unique id
     */
    String getId();

    /**
     * Get display name for this coordinate format
     * @return display name (localized)
     */
    String getDisplayName();

    /**
     * Convert latitude to string.
     * @param ll the coordinate
     * @return formatted latitude
     */
    String latToString(ILatLon ll);

    /**
     * Convert longitude to string.
     * @param ll the coordinate
     * @return formatted longitude
     */
    String lonToString(ILatLon ll);

    /**
     * Convert the coordinate to string: latitude + separator + longitude
     * @param ll the coordinate
     * @param separator the separator
     * @return formatted coordinate
     */
    default String toString(ILatLon ll, String separator) {
        return latToString(ll) + separator + lonToString(ll);
    }
}
