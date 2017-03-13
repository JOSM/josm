// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.gui.mappaint.Keyword;

/**
 * This strategy defines how to place a label or icon inside the area.
 * @since 11720
 */
public interface PositionForAreaStrategy {
    /**
     * Finds the correct position of a label / icon inside the area.
     * @param area The area to search in
     * @param nb The bounding box of the thing we are searching a place for.
     * @return The position as rectangle with the same dimension as nb. <code>null</code> if none was found.
     */
    Rectangle2D findLabelPlacement(Shape area, Rectangle2D nb);

    /**
     * Checks whether this placement strategy supports more detailed (rotation / ...) placement using a glyph vecotr.
     * @return <code>true</code> if it is supported.
     */
    boolean supportsGlyphVector();

    /**
     * Gets a strategy for the given keyword.
     * @param keyword The text position keyword.
     * @return The strategy or line if none was specified.
     * @since 11720
     */
    public static PositionForAreaStrategy forKeyword(Keyword keyword) {
        return forKeyword(keyword, LINE);
    }

    /**
     * Gets a strategy for the given keyword.
     * @param keyword The text position keyword.
     * @param defaultStrategy The default if no strategy was recognized.
     * @return The strategy or line if none was specified.
     * @since 11720
     */
    public static PositionForAreaStrategy forKeyword(Keyword keyword, PositionForAreaStrategy defaultStrategy) {
        if (keyword == null) {
            return defaultStrategy;
        }
        switch (keyword.val) {
        case "center":
            return PARTIALY_INSIDE;
        case "inside":
            return INSIDE;
        case "line":
            return LINE;
        default:
            return defaultStrategy;
        }
    }

    /**
     * Places the label onto the line.
     *
     * @since 11720
     */
    public static PositionForAreaStrategy LINE = new OnLineStrategy();

    /**
     * Places the label / icon so that it is completely inside the area.
     *
     * @since 11720
     */
    public static PositionForAreaStrategy INSIDE = new CompletelyInsideAreaStrategy();

    /**
     * Places the label / icon so that is is on the area.
     * @since 11720
     */
    public static PositionForAreaStrategy PARTIALY_INSIDE = new PartialyInsideAreaStrategy();

    /**
     * Places the label onto the line.
     *
     * @since 11720
     */
    class OnLineStrategy implements PositionForAreaStrategy {
        @Override
        public Rectangle2D findLabelPlacement(Shape area, Rectangle2D nb) {
            // cannot place inside area.
            return null;
        }

        @Override
        public boolean supportsGlyphVector() {
            return true;
        }
    };

    /**
     * Places the label / icon so that it is completely inside the area.
     *
     * @since 11720
     */
    class CompletelyInsideAreaStrategy implements PositionForAreaStrategy {

        @Override
        public Rectangle2D findLabelPlacement(Shape area, Rectangle2D nb) {
            // Using the Centroid is Nicer for buildings like: +--------+
            // but this needs to be fast.  As most houses are  |   42   |
            // boxes anyway, the center of the bounding box    +---++---+
            // will have to do.                                    ++
            // Centroids are not optimal either, just imagine a U-shaped house.

            Rectangle pb = area.getBounds();

            // quick check to see if label box is smaller than primitive box
            if (pb.width < nb.getWidth() || pb.height < nb.getHeight()) {
                return null;
            }

            final double w = pb.width - nb.getWidth();
            final double h = pb.height - nb.getHeight();

            final int x2 = pb.x + (int) (w / 2.0);
            final int y2 = pb.y + (int) (h / 2.0);

            final int nbw = (int) nb.getWidth();
            final int nbh = (int) nb.getHeight();

            Rectangle centeredNBounds = new Rectangle(x2, y2, nbw, nbh);

            // slower check to see if label is displayed inside primitive shape
            if (area.contains(centeredNBounds)) {
                return centeredNBounds;
            }

            // if center position (C) is not inside osm shape, try naively some other positions as follows:
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            final int x1 = pb.x + (int) (w / 4.0);
            final int x3 = pb.x + (int) (3 * w / 4.0);
            final int y1 = pb.y + (int) (h / 4.0);
            final int y3 = pb.y + (int) (3 * h / 4.0);
            // CHECKSTYLE.ON: SingleSpaceSeparator
            // +-----------+
            // |  5  1  6  |
            // |  4  C  2  |
            // |  8  3  7  |
            // +-----------+
            Rectangle[] candidates = new Rectangle[] { new Rectangle(x2, y1, nbw, nbh), new Rectangle(x3, y2, nbw, nbh),
                    new Rectangle(x2, y3, nbw, nbh), new Rectangle(x1, y2, nbw, nbh), new Rectangle(x1, y1, nbw, nbh),
                    new Rectangle(x3, y1, nbw, nbh), new Rectangle(x3, y3, nbw, nbh), new Rectangle(x1, y3, nbw, nbh) };
            // Dumb algorithm to find a better placement. We could surely find a smarter one but it should
            // solve most of building issues with only few calculations (8 at most)
            for (int i = 0; i < candidates.length; i++) {
                centeredNBounds = candidates[i];
                if (area.contains(centeredNBounds)) {
                    return centeredNBounds;
                }
            }

            // none found
            return null;
        }

        @Override
        public boolean supportsGlyphVector() {
            return false;
        }
    }

    /**
     * A strategy that places the label / icon so that is is on the area.
     *
     * The center of that place should be in the area, but the icon / label may overlap on the edges.
     * @since 11720
     */
    public class PartialyInsideAreaStrategy extends CompletelyInsideAreaStrategy {
        @Override
        public Rectangle2D findLabelPlacement(Shape area, Rectangle2D nb) {
            Rectangle2D inside = super.findLabelPlacement(area, nb);
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
                Rectangle2D position = super.findLabelPlacement(area, smallNb);
                if (position == null) {
                    return null;
                }
                return new Rectangle2D.Double(position.getX() - nbdx, position.getY() - nbdy, nb.getWidth(),
                        nb.getHeight());
            }
        }
    }

}
