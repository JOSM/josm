// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Simple data class that keeps map center and scale in one object.
 * @since 5670 (creation)
 * @since 6992 (extraction in this package) 
 */
public class ViewportData {
    private EastNorth center;
    private Double scale;

    /**
     * Constructs a new {@code ViewportData}.
     * @param center Projected coordinates of the map center
     * @param scale Scale factor in east-/north-units per pixel
     */
    public ViewportData(EastNorth center, Double scale) {
        this.center = center;
        this.scale = scale;
    }

    /**
     * Return the projected coordinates of the map center
     * @return the center
     */
    public EastNorth getCenter() {
        return center;
    }

    /**
     * Return the scale factor in east-/north-units per pixel.
     * @return the scale
     */
    public Double getScale() {
        return scale;
    }
}
