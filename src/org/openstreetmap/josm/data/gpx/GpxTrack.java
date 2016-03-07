// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Read-only gpx track. Implementations doesn't have to be immutable, but should always be thread safe.
 * @since 444
 */
public interface GpxTrack extends IWithAttributes {

    /**
     * Returns the track segments.
     * @return the track segments
     */
    Collection<GpxTrackSegment> getSegments();

    /**
     * Returns the track attributes.
     * @return the track attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Returns the track bounds.
     * @return the track bounds
     */
    Bounds getBounds();

    /**
     * Returns the track length.
     * @return the track length
     */
    double length();

    /**
     * Returns the number of times this track has been changed.
     * @return Number of times this track has been changed. Always 0 for read-only tracks
     */
    int getUpdateCount();
}
