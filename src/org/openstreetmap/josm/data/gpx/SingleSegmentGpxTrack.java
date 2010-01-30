package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;

public class SingleSegmentGpxTrack implements GpxTrack {

	private final Map<String, Object> attributes;
	private final GpxTrackSegment trackSegment;

	public SingleSegmentGpxTrack(GpxTrackSegment trackSegment, Map<String, Object> attributes) {
		this.attributes = Collections.unmodifiableMap(attributes);
		this.trackSegment = trackSegment;
	}


	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Bounds getBounds() {
		return trackSegment.getBounds();
	}

	public Collection<GpxTrackSegment> getSegments() {
		return Collections.singleton(trackSegment);
	}

	public double length() {
		return trackSegment.length();
	}

}
