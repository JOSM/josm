//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GpxTrack extends WithAttributes {
    public final Collection<Collection<WayPoint>> trackSegs
    = new ConcurrentLinkedQueue<Collection<WayPoint>>();

    /**
     * calculates the length of the track
     */
    public double length(){
        double result = 0.0; // in meters
        WayPoint last = null;

        for (Collection<WayPoint> trkseg : trackSegs) {
            for (WayPoint tpt : trkseg) {
                if(last != null){
                    Double d = last.getCoor().greatCircleDistance(tpt.getCoor());
                    if(!d.isNaN() && !d.isInfinite()) {
                        result += d;
                    }
                }
                last = tpt;
            }
            last = null; // restart for each track segment
        }
        return result;
    }
}
