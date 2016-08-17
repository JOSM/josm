// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.geom.Path2D;

import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * An extension of {@link Path2D} with special methods for map positions.
 * @author Michael Zangl
 * @since 10827
 */
public class MapPath2D extends Path2D.Double {
    /**
     * Create a new, empty path.
     */
    public MapPath2D() {
        // no default definitions
    }

    /**
     * Move the path to the view position of given point
     * @param p The point
     */
    public void moveTo(MapViewPoint p) {
        moveTo(p.getInViewX(), p.getInViewY());
    }

    /**
     * Draw a line to the view position of given point
     * @param p The point
     */
    public void lineTo(MapViewPoint p) {
        lineTo(p.getInViewX(), p.getInViewY());
    }
}
