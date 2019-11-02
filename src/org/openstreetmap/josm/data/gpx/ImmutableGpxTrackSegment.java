// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;

/**
 * A gpx track segment consisting of multiple waypoints, that cannot be changed.
 * @deprecated Use {@link GpxTrackSegment} instead!
 */
@Deprecated
public class ImmutableGpxTrackSegment extends GpxTrackSegment {

    /**
     * Constructs a new {@code ImmutableGpxTrackSegment}.
     * @param wayPoints list of waypoints
     */
    public ImmutableGpxTrackSegment(Collection<WayPoint> wayPoints) {
        super(wayPoints);
    }
}
