// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.Bounds;

public class ImmutableGpxTrackSegment implements GpxTrackSegment {

    private final Collection<WayPoint> wayPoints;
    private final Bounds bounds;
    private final double length;

    public ImmutableGpxTrackSegment(Collection<WayPoint> wayPoints) {
        this.wayPoints = Collections.unmodifiableCollection(new ArrayList<WayPoint>(wayPoints));
        this.bounds = calculateBounds();
        this.length = calculateLength();
    }

    private Bounds calculateBounds() {
        Bounds result = null;
        for (WayPoint wpt: wayPoints) {
            if (result == null) {
                result = new Bounds(wpt.getCoor());
            } else {
                result.extend(wpt.getCoor());
            }
        }
        return result;
    }

    private double calculateLength() {
        double result = 0.0; // in meters
        WayPoint last = null;
        for (WayPoint tpt : wayPoints) {
            if(last != null){
                Double d = last.getCoor().greatCircleDistance(tpt.getCoor());
                if(!d.isNaN() && !d.isInfinite()) {
                    result += d;
                }
            }
            last = tpt;
        }
        return result;
    }

    @Override
    public Bounds getBounds() {
        if (bounds == null)
            return null;
        else
            return new Bounds(bounds);
    }

    @Override
    public Collection<WayPoint> getWayPoints() {
        return wayPoints;
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public int getUpdateCount() {
        return 0;
    }

}
