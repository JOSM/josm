// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;

/**
 * Factory creating TileLoaders for layers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
@FunctionalInterface
public interface TileLoaderFactory {

    /**
     * Creates TileLoaderFactory - factory that creates tile loaders with all options already set
     *
     * @param listener that will be notified, when tile has finished loading
     * @param headers that will be sent with requests to TileSource. <code>null</code> indicates none
     * @param minimumExpiryTime minimum expiry time
     * @return TileLoader that uses both of above
     */
    TileLoader makeTileLoader(TileLoaderListener listener, Map<String, String> headers, long minimumExpiryTime);
}
