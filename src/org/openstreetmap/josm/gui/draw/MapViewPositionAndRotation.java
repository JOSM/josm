// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * A map view point combined with a rotation angle.
 *
 * @author Michael Zangl
 * @since 11748
 */
public class MapViewPositionAndRotation {

    private final MapViewPoint point;

    private final double theta;

    /**
     * Create a new {@link MapViewPositionAndRotation}
     * @param point the point
     * @param theta the rotation
     */
    public MapViewPositionAndRotation(MapViewPoint point, double theta) {
        super();
        this.point = point;
        this.theta = theta;
    }

    /**
     * Gets the point.
     * @return The point
     */
    public MapViewPoint getPoint() {
        return point;
    }

    /**
     * Gets the rotation
     * @return the rotation
     */
    public double getRotation() {
        return theta;
    }

    @Override
    public String toString() {
        return "MapViewPositionAndRotation [" + point + ", theta=" + theta + "]";
    }
}
