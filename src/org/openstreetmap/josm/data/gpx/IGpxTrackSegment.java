// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;

import org.openstreetmap.josm.data.Bounds;

/**
 * Read-only gpx track segments. Implementations don't have to be immutable, but should always be thread safe.
 * @since 15496
 */
public interface IGpxTrackSegment extends IWithAttributes {

    /**
     * Returns the segment bounds.
     * @return the segment bounds
     */
    Bounds getBounds();

    /**
     * Returns the segment waypoints.
     * @return the segment waypoints
     */
    Collection<WayPoint> getWayPoints();

    /**
     * Returns the segment length.
     * @return the segment length
     */
    double length();

    /**
     * Returns the number of times this track has been changed
     * @return Number of times this track has been changed. Always 0 for read-only segments
     */
    int getUpdateCount();
}
