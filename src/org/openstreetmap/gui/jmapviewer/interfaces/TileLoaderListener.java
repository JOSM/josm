// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

import org.openstreetmap.gui.jmapviewer.Tile;

public interface TileLoaderListener {

    /**
     * Will be called if a new {@link Tile} has been loaded successfully.
     * Loaded can mean downloaded or loaded from file cache.
     *
     * @param tile The tile
     * @param success {@code true} if the tile has been loaded successfully, {@code false} otherwise
     */
    public void tileLoadingFinished(Tile tile, boolean success);
}
