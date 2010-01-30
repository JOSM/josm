//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;


/**
 * Read-only gpx track. Implementations doesn't have to be immutable, but should always be thread safe.
 *
 */

public interface GpxTrack {

    Collection<GpxTrackSegment> getSegments();
    Map<String, Object> getAttributes();
    Bounds getBounds();
    double length();

}
