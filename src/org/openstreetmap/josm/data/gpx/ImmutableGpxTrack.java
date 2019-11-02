// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * GPX track, NOT immutable
 * @since 2907
 * @deprecated Use {@link GpxTrack} instead!
 */
@Deprecated
public class ImmutableGpxTrack extends GpxTrack {

    /**
     * Constructs a new {@code ImmutableGpxTrack}.
     * @param trackSegs track segments
     * @param attributes track attributes
     */
    public ImmutableGpxTrack(Collection<Collection<WayPoint>> trackSegs, Map<String, Object> attributes) {
        super(trackSegs, attributes);
    }

    /**
     * Constructs a new {@code ImmutableGpxTrack} from {@code GpxTrackSegment} objects.
     * @param segments The segments to build the track from.  Input is not deep-copied,
     *                 which means the caller may reuse the same segments to build
     *                 multiple ImmutableGpxTrack instances from.  This should not be
     *                 a problem, since this object cannot modify {@code this.segments}.
     * @param attributes Attributes for the GpxTrack, the input map is copied.
     * @since 13210
     */
    public ImmutableGpxTrack(List<IGpxTrackSegment> segments, Map<String, Object> attributes) {
        super(segments, attributes);
    }
}
