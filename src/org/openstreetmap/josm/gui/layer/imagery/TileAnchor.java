// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;

/**
 * Class that fixes the position of a tile in a given coordinate space.
 *
 * This is done by storing the coordinates of the tile origin and the opposite
 * tile corner.
 * <p>
 * It may represent a reprojected tile, i.e. the tile is rotated / deformed in an
 * arbitrary way. In general, the tile origin cannot be expected to be the
 * upper left corner of the rectangle that is spanned by the 2 points.
 * <p>
 * The coordinate space may be
 * <ul>
 *   <li>pixel coordinates of the image file</li>
 *   <li>projected coordinates (east / north)</li>
 *   <li>screen pixel coordinates</li>
 * </ul>
 * @since 11846
 */
public class TileAnchor {

    protected final Point2D tileOrigin, nextTileOrigin;

    /**
     * Create a new tile anchor.
     * @param tileOrigin position of the tile origin
     * @param nextTileOrigin position of the opposite tile corner, i.e. the
     * origin of the tile with index (x+1,y+1), when current tile has index (x,y)
     */
    public TileAnchor(Point2D tileOrigin, Point2D nextTileOrigin) {
        this.tileOrigin = tileOrigin;
        this.nextTileOrigin = nextTileOrigin;
    }

    /**
     * Constructs a new {@code TileAnchor}.
     * @param tileOrigin position of the tile origin
     * @param nextTileOrigin position of the opposite tile corner, i.e. the
     * origin of the tile with index (x+1,y+1), when current tile has index (x,y)
     */
    public TileAnchor(IProjected tileOrigin, IProjected nextTileOrigin) {
        this.tileOrigin = new Point2D.Double(tileOrigin.getEast(), tileOrigin.getNorth());
        this.nextTileOrigin = new Point2D.Double(nextTileOrigin.getEast(), nextTileOrigin.getNorth());
    }

    /**
     * Returns the position of the tile origin.
     * @return the position of the tile origin
     */
    public Point2D getTileOrigin() {
        return tileOrigin;
    }

    /**
     * Returns the position of the opposite tile corner.
     * @return the position of the opposite tile corner, i.e. the
     * origin of the tile with index (x+1,y+1), when current tile has index (x,y)
     */
    public Point2D getNextTileOrigin() {
        return nextTileOrigin;
    }

    @Override
    public String toString() {
        return "TileAnchor{" + tileOrigin + "; " + nextTileOrigin + '}';
    }

    /**
     * Create a transformation that converts points from this coordinate space
     * to another coordinate space.
     * @param other tile anchor of the tile in the target coordinate space
     * @return affine transformation from this coordinate space to the target
     * coordinate space
     */
    public AffineTransform convert(TileAnchor other) {
        Point2D src1 = this.getTileOrigin();
        Point2D src2 = this.getNextTileOrigin();
        Point2D dest1 = other.getTileOrigin();
        Point2D dest2 = other.getNextTileOrigin();

        double scaleX = (dest2.getX() - dest1.getX()) / (src2.getX() - src1.getX());
        double scaleY = (dest2.getY() - dest1.getY()) / (src2.getY() - src1.getY());
        double offsetX0 = dest1.getX() - scaleX * src1.getX();
        double offsetY0 = dest1.getY() - scaleY * src1.getY();
        return new AffineTransform(scaleX, 0, 0, scaleY, offsetX0, offsetY0);
    }
}
