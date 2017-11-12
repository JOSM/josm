// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

import org.openstreetmap.gui.jmapviewer.Tile;

/**
 * Interface for implementing a tile loader. Tiles are usually loaded via HTTP
 * or from a file.
 *
 * @author Jan Peter Stotz
 */
public interface TileLoader {

    /**
     * A typical implementation of this function should create and return a
     * new {@link TileJob} instance that performs the load action.
     *
     * @param tile the tile to be loaded
     * @return {@link TileJob} implementation that performs the desired load
     *          action.
     */
    TileJob createTileLoaderJob(Tile tile);

    /**
     * cancels all outstanding tasks in the queue. This should rollback the state of the tiles in the queue
     * to loading = false / loaded = false
     */
    void cancelOutstandingTasks();

    /**
     * Determines whether this {@link TileLoader} has tasks which have not completed.
     * @return whether this {@link TileLoader} has tasks which have not completed. This answer may well be
     * "approximate" given that many implementations will be using mechanisms where a queue's state can change
     * during the computation.
     */
    default boolean hasOutstandingTasks() {
        // default implementation supplied just to make transition easier for external implementors
        throw new UnsupportedOperationException("Not implemented");
    }
}
