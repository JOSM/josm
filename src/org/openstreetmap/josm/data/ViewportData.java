// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Data class to keep viewport information.
 *
 * This can be either a combination of map center and map scale or
 * a rectangle in east-north coordinate space.
 *
 * Either of those will be null, so the consumer of the ViewportData
 * object has to check, which one is set.
 *
 * @since 5670 (creation)
 * @since 6992 (extraction in this package)
 */
public class ViewportData {
    private final EastNorth center;
    private final Double scale;

    private final ProjectionBounds bounds;

    /**
     * Constructs a new {@code ViewportData}.
     * @param center Projected coordinates of the map center
     * @param scale Scale factor in east-/north-units per pixel
     */
    public ViewportData(EastNorth center, Double scale) {
        CheckParameterUtil.ensureParameterNotNull(center);
        CheckParameterUtil.ensureParameterNotNull(scale);
        this.center = center;
        this.scale = scale;
        this.bounds = null;
    }

    /**
     * Create a new {@link ViewportData}
     * @param bounds The bounds to zoom to
     */
    public ViewportData(ProjectionBounds bounds) {
        CheckParameterUtil.ensureParameterNotNull(bounds);
        this.center = null;
        this.scale = null;
        this.bounds = bounds;
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

    /**
     * Return the bounds in east-north coordinate space.
     * @return the bounds
     */
    public ProjectionBounds getBounds() {
        return bounds;
    }
}
