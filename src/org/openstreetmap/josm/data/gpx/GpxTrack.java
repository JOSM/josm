// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Read-only gpx track. Implementations doesn't have to be immutable, but should always be thread safe.
 *
 */
public interface GpxTrack extends IWithAttributes {

    Collection<GpxTrackSegment> getSegments();
    Map<String, Object> getAttributes();
    Bounds getBounds();
    double length();
    
    /**
     *
     * @return Number of times this track has been changed. Always 0 for read-only tracks
     */
    int getUpdateCount();
}
