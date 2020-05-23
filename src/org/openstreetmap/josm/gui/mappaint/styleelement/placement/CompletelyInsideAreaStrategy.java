// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement.placement;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation;

/**
 * Places the label / icon so that it is completely inside the area.
 *
 * @author Michael Zangl
 * @since 11722
 * @since 11748 moved to own file
 */
public class CompletelyInsideAreaStrategy implements PositionForAreaStrategy {
    /**
     * An instance of this class.
     */
    public static final CompletelyInsideAreaStrategy INSTANCE = new CompletelyInsideAreaStrategy(0, 0);

    protected final double offsetX;
    protected final double offsetY;

    protected CompletelyInsideAreaStrategy(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public MapViewPositionAndRotation findLabelPlacement(MapViewPath path, Rectangle2D nb) {
        // Using the Centroid is Nicer for buildings like: +--------+
        // but this needs to be fast.  As most houses are  |   42   |
        // boxes anyway, the center of the bounding box    +---++---+
        // will have to do.                                    ++
        // Centroids are not optimal either, just imagine a U-shaped house.

        Rectangle pb = path.getBounds();

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
        if (path.contains(centeredNBounds)) {
            return centerOf(path.getMapViewState(), centeredNBounds);
        }

        // if center position (C) is not inside osm shape, try naively some other positions as follows:
        final int x1 = pb.x + (int) (.25 * w);
        final int x3 = pb.x + (int) (.75 * w);
        final int y1 = pb.y + (int) (.25 * h);
        final int y3 = pb.y + (int) (.75 * h);
        // +-----------+
        // |  5  1  6  |
        // |  4  C  2  |
        // |  8  3  7  |
        // +-----------+
        Rectangle[] candidates = {
                new Rectangle(x2, y1, nbw, nbh),
                new Rectangle(x3, y2, nbw, nbh),
                new Rectangle(x2, y3, nbw, nbh),
                new Rectangle(x1, y2, nbw, nbh),
                new Rectangle(x1, y1, nbw, nbh),
                new Rectangle(x3, y1, nbw, nbh),
                new Rectangle(x3, y3, nbw, nbh),
                new Rectangle(x1, y3, nbw, nbh)
        };
        // Dumb algorithm to find a better placement. We could surely find a smarter one but it should
        // solve most of building issues with only few calculations (8 at most)
        for (int i = 0; i < candidates.length; i++) {
            centeredNBounds = candidates[i];
            if (path.contains(centeredNBounds)) {
                return centerOf(path.getMapViewState(), centeredNBounds);
            }
        }

        // none found
        return null;
    }

    private MapViewPositionAndRotation centerOf(MapViewState mapViewState, Rectangle centeredNBounds) {
        double x = centeredNBounds.getCenterX() + offsetX;
        double y = centeredNBounds.getCenterY() + offsetY;
        return new MapViewPositionAndRotation(mapViewState.getForView(x, y), 0);
    }

    @Override
    public boolean supportsGlyphVector() {
        return false;
    }

    @Override
    public PositionForAreaStrategy withAddedOffset(Point2D addToOffset) {
        if (Math.abs(addToOffset.getX()) < 1e-5 && Math.abs(addToOffset.getY()) < 1e-5) {
            return this;
        } else {
            return new CompletelyInsideAreaStrategy(offsetX + addToOffset.getX(), offsetY - addToOffset.getY());
        }
    }

    @Override
    public String toString() {
        return "CompletelyInsideAreaStrategy [offsetX=" + offsetX + ", offsetY=" + offsetY + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(offsetX, offsetY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CompletelyInsideAreaStrategy other = (CompletelyInsideAreaStrategy) obj;
        return Double.doubleToLongBits(offsetX) == Double.doubleToLongBits(other.offsetX)
                && Double.doubleToLongBits(offsetY) == Double.doubleToLongBits(other.offsetY);
    }
}
