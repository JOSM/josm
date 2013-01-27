// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

public class ImmutableGpxTrack extends WithAttributes implements GpxTrack {

    private final Collection<GpxTrackSegment> segments;
    private final double length;
    private final Bounds bounds;

    public ImmutableGpxTrack(Collection<Collection<WayPoint>> trackSegs, Map<String, Object> attributes) {
        List<GpxTrackSegment> newSegments = new ArrayList<GpxTrackSegment>();
        for (Collection<WayPoint> trackSeg: trackSegs) {
            if (trackSeg != null && !trackSeg.isEmpty()) {
                newSegments.add(new ImmutableGpxTrackSegment(trackSeg));
            }
        }
        this.attr = Collections.unmodifiableMap(new HashMap<String, Object>(attributes));
        this.segments = Collections.unmodifiableCollection(newSegments);
        this.length = calculateLength();
        this.bounds = calculateBounds();
    }

    private double calculateLength(){
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
        if (bounds == null)
            return null;
        else
            return new Bounds(bounds);
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
    public int getUpdateCount() {
        return 0;
    }
}
