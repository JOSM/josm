// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import java.awt.geom.Path2D;

import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * An extension of {@link Path2D} with special methods for map positions.
 * @author Michael Zangl
 * @since 10875
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
     * @return this for easy chaining.
     */
    public MapPath2D moveTo(MapViewPoint p) {
        moveTo(p.getInViewX(), p.getInViewY());
        return this;
    }

    /**
     * Draw a line to the view position of given point
     * @param p The point
     * @return this for easy chaining.
     */
    public MapPath2D lineTo(MapViewPoint p) {
        lineTo(p.getInViewX(), p.getInViewY());
        return this;
    }

    /**
     * Add the given shape centered around the given point
     * @param p The point to draw around
     * @param symbol The symbol type
     * @param size The size of the symbol in pixel
     * @return this for easy chaining.
     */
    public MapPath2D shapeAround(MapViewPoint p, SymbolShape symbol, double size) {
        append(symbol.shapeAround(p.getInViewX(), p.getInViewY(), size), false);
        return this;
    }
}
