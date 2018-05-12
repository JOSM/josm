// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * Tileloader for WMS based imagery. It is separate to use different ThreadPoolExecutor, as we want
 * to define number of simultaneous downloads for WMS separately
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class WMSCachedTileLoader extends TMSCachedTileLoader {

    /**
     * overrides the THREAD_LIMIT in superclass, as we want to have separate limit and pool for WMS
     */
    public static final IntegerProperty THREAD_LIMIT = new IntegerProperty("imagery.wms.loader.maxjobs", 3);

    /**
     * Creates a TileLoader with separate WMS downloader.
     *
     * @param listener that will be notified when tile is loaded
     * @param cache reference
     * @param connectTimeout to tile source
     * @param readTimeout from tile source
     * @param headers to be sent with requests
     */
    public WMSCachedTileLoader(TileLoaderListener listener, ICacheAccess<String, BufferedImageCacheEntry> cache,
            TileJobOptions options) {

        super(listener, cache, options);
        setDownloadExecutor(TMSCachedTileLoader.getNewThreadPoolExecutor("WMS-downloader-%d", THREAD_LIMIT.get()));
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        return new WMSCachedTileLoaderJob(listener, tile, cache, options, getDownloadExecutor());
    }
}
