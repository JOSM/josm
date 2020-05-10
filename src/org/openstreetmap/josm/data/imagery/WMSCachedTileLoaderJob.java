// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;

/**
 * Separate class to handle WMS jobs, as it needs to react differently to HTTP response codes from WMS server
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class WMSCachedTileLoaderJob extends TMSCachedTileLoaderJob {

    /**
     * Creates a job - that will download specific tile
     * @param listener will be notified, when tile has loaded
     * @param tile to load
     * @param cache to use (get/put)
     * @param options options for tile job
     * @param downloadExecutor that will execute the download task (if needed)
     */
    public WMSCachedTileLoaderJob(TileLoaderListener listener,
            Tile tile,
            ICacheAccess<String, BufferedImageCacheEntry> cache,
            TileJobOptions options,
            ThreadPoolExecutor downloadExecutor) {
        super(listener, tile, cache, options, downloadExecutor);
    }

    @Override
    public String getCacheKey() {
        // include projection in cache key, as with different projections different response will be returned from server
        String key = super.getCacheKey();
        if (key != null) {
            return key + tile.getSource().getServerCRS();
        }
        return null;
    }
}
