package org.openstreetmap.gui.jmapviewer.interfaces;

import org.openstreetmap.gui.jmapviewer.Tile;

//License: GPL. Copyright 2008 by Jan Peter Stotz

public interface TileLoaderListener {

    /**
     * Will be called if a new {@link Tile} has been loaded successfully.
     * Loaded can mean downloaded or loaded from file cache.
     *
     * @param tile
     */
    public void tileLoadingFinished(Tile tile, boolean success);

    public TileCache getTileCache();
}
