// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;

/**
 * Factory creating TileLoaders for layers
 *
 * @author Wiktor Niesiobędzki
 * @since TODO
 *
 */
public interface TileLoaderFactory {

    /**
     * @param listener that will be notified, when tile has finished loading
     * @return TileLoader that notifies specified listener
     */
    TileLoader makeTileLoader(TileLoaderListener listener);

    /**
     * @param listener that will be notified, when tile has finished loading
     * @param headers that will be sent with requests to TileSource
     * @return TileLoader that uses both of above
     */
    TileLoader makeTileLoader(TileLoaderListener listener, Map<String, String> headers);

}
