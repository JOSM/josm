// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.draw.MapPath2D;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class helps with painting arrows with fixed length along a path.
 * @author Michael Zangl
 * @since 10827
 */
public class ArrowPaintHelper {
    private final double sin;
    private final double cos;
    private final double length;

    /**
     * Creates a new arrow helper.
     * @param radians The angle of the arrow. 0 means that it lies on the current line. In radians
     * @param length The length of the arrow lines.
     */
    public ArrowPaintHelper(double radians, double length) {
        this.sin = Math.sin(radians);
        this.cos = Math.cos(radians);
        this.length = length;
    }

    /**
     * Paint the arrow
     * @param path The path to append the arrow to.
     * @param point The point to paint the tip at
     * @param fromDirection The direction the line is coming from.
     */
    public void paintArrowAt(MapPath2D path, MapViewPoint point, MapViewPoint fromDirection) {
        double x = point.getInViewX();
        double y = point.getInViewY();
        double dx = fromDirection.getInViewX() - x;
        double dy = fromDirection.getInViewY() - y;
        double norm = Math.sqrt(dx * dx + dy * dy);
        if (norm > 1e-10) {
            dx *= length / norm;
            dy *= length / norm;
            path.moveTo(x + dx * cos + dy * sin, y + dx * -sin + dy * cos);
            if (!Utils.equalsEpsilon(cos, 0)) {
                path.lineTo(point);
            }
            path.lineTo(x + dx * cos + dy * -sin, y + dx * sin + dy * cos);
        }
    }

    /**
     * Gets the length of the arrow along the line segment.
     * @return the length along the line
     * @since 12154
     */
    public double getOnLineLength() {
        return length * cos;
    }
}
