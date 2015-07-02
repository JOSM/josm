// License: GPL. For details, see LICENSE file.
package org.openstreetmap.gui.jmapviewer;

/**
 * @author w
 *
 */
public class TileXY {
    /**
     * x index of the tile (horizontal)
     */
    private double x;
    /**
     * y number of the tile (vertical)
     */
    private double y;

    /**
     * Returns an instance of coordinates.
     *
     * @param d number of the tile
     * @param e number of the tile
     */
    public TileXY(double d, double e) {
        this.x = d;
        this.y = e;
    }

    /**
     * @return x index of the tile as integer
     */
    public int getXIndex() {
        return x < 0 ? (int) Math.ceil(x) : (int) Math.floor(x);
    }

    /**
     * @return y index of the tile as integer
     */
    public int getYIndex() {
        return y < 0 ? (int) Math.ceil(x) : (int) Math.floor(y);
    }

    /**
     * @return x index as double, might be non integral, when the point is not topleft corner of the tile
     */
    public double getX() {
        return x;
    }

    /**
     * @return y index as double, might be non integral, when the point is not topleft corner of the tile
     */
    public double getY() {
        return y;
    }

}
