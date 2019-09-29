// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Geometry;

/**
 * A class to find the distance between an {@link OsmPrimitive} and a GPX point.
 *
 * @author Taylor Smock
 * @since 14802
 */
public final class GpxDistance {
    private GpxDistance() {
        // This class should not be instantiated
    }

    /**
     * Find the distance between a point and a dataset of surveyed points
     * @param p OsmPrimitive from which to get the lowest distance to a GPX point
     * @param gpxData Data from which to get the GPX points
     * @return The shortest distance
     */
    public static double getLowestDistance(OsmPrimitive p, GpxData gpxData) {
        return gpxData.getTrackPoints()
                .mapToDouble(tp -> Geometry.getDistance(p, new Node(tp.getCoor())))
                .filter(x -> x >= 0)
                .min().orElse(Double.MAX_VALUE);
    }
}
