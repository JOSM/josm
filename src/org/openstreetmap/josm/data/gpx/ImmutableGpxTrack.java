// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Immutable GPX track.
 * @since 2907
 */
public class ImmutableGpxTrack extends WithAttributes implements GpxTrack {

    private final List<GpxTrackSegment> segments;
    private final double length;
    private final Bounds bounds;

    /**
     * Constructs a new {@code ImmutableGpxTrack}.
     * @param trackSegs track segments
     * @param attributes track attributes
     */
    public ImmutableGpxTrack(Collection<Collection<WayPoint>> trackSegs, Map<String, Object> attributes) {
        List<GpxTrackSegment> newSegments = new ArrayList<>();
        for (Collection<WayPoint> trackSeg: trackSegs) {
            if (trackSeg != null && !trackSeg.isEmpty()) {
                newSegments.add(new ImmutableGpxTrackSegment(trackSeg));
            }
        }
        this.attr = Collections.unmodifiableMap(new HashMap<>(attributes));
        this.segments = Collections.unmodifiableList(newSegments);
        this.length = calculateLength();
        this.bounds = calculateBounds();
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
    public ImmutableGpxTrack(List<GpxTrackSegment> segments, Map<String, Object> attributes) {
        this.attr = Collections.unmodifiableMap(new HashMap<>(attributes));
        this.segments = Collections.unmodifiableList(segments);
        this.length = calculateLength();
        this.bounds = calculateBounds();
    }

    private double calculateLength() {
        double result = 0.0; // in meters

        for (GpxTrackSegment trkseg : segments) {
            result += trkseg.length();
        }
        return result;
    }

    private Bounds calculateBounds() {
        Bounds result = null;
        for (GpxTrackSegment segment: segments) {
            Bounds segBounds = segment.getBounds();
            if (segBounds != null) {
                if (result == null) {
                    result = new Bounds(segBounds);
                } else {
                    result.extend(segBounds);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attr;
    }

    @Override
    public Bounds getBounds() {
        return bounds == null ? null : new Bounds(bounds);
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public Collection<GpxTrackSegment> getSegments() {
        return segments;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + ((segments == null) ? 0 : segments.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableGpxTrack other = (ImmutableGpxTrack) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!segments.equals(other.segments))
            return false;
        return true;
    }
}
