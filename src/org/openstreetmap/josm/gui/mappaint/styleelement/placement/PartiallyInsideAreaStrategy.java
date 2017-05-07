// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement.placement;

import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation;

/**
 * A strategy that places the label / icon so that is is on the area.
 *
 * The center of that place should be in the area, but the icon / label may overlap on the edges.
 *
 * @author Michael Zangl
 * @since 11722
 * @since 11748 moved to own file
 */
public class PartiallyInsideAreaStrategy extends CompletelyInsideAreaStrategy {
    /**
     * An instance of this class.
     */
    public static final PartiallyInsideAreaStrategy INSTANCE = new PartiallyInsideAreaStrategy();

    private PartiallyInsideAreaStrategy() {
    }

    @Override
    public MapViewPositionAndRotation findLabelPlacement(MapViewPath path, Rectangle2D nb) {
        MapViewPositionAndRotation inside = super.findLabelPlacement(path, nb);
        if (inside != null) {
            return inside;
        }

        double nbdx = Math.max(0, (nb.getWidth() - 20) / 2);
        double nbdy = Math.max(0, (nb.getHeight() - 10) / 2);

        if (nbdx < .5 && nbdy < .5) {
            // we can't do any better
            return null;
        } else {
            Rectangle2D smallNb = new Rectangle2D.Double(nb.getX() + nbdx, nb.getY() + nbdy,
                    nb.getWidth() - 2 * nbdx, nb.getHeight() - 2 * nbdy);
            return super.findLabelPlacement(path, smallNb);
        }
    }
}
