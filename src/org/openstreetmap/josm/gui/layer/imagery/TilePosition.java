// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import org.openstreetmap.gui.jmapviewer.Tile;

/**
 * The position of a single tile.
 * @author Michael Zangl
 */
public class TilePosition {
    private final int x;
    private final int y;
    private final int zoom;

    /**
     * Constructs a new {@code TilePosition}.
     * @param x X coordinate
     * @param y Y coordinate
     * @param zoom zoom level
     */
    public TilePosition(int x, int y, int zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
    }

    /**
     * Constructs a new {@code TilePosition}.
     * @param tile tile
     */
    public TilePosition(Tile tile) {
        this(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    /**
     * Returns the x position.
     * @return the x position
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y position.
     * @return the y position
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the zoom.
     * @return the zoom
     */
    public int getZoom() {
        return zoom;
    }

    @Override
    public String toString() {
        return "TilePosition [x=" + x + ", y=" + y + ", zoom=" + zoom + ']';
    }
}
