// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;

import org.openstreetmap.josm.data.Bounds;

/**
 * Read-only gpx track segments. Implementations doesn't have to be immutable, but should always be thread safe.
 *
 */
public interface GpxTrackSegment {

    Bounds getBounds();
    Collection<WayPoint> getWayPoints();
    double length();
    /**
     *
     * @return Number of times this track has been changed. Always 0 for read-only segments
     */
    int getUpdateCount();
}
