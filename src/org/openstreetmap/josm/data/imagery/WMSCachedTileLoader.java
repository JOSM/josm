// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * Tileloader for WMS based imagery. It is separate to use different ThreadPoolExecutor, as we want
 * to define number of simultaneous downloads for WMS separately
 *
 * @author Wiktor Niesiobędzki
 * @since TODO
 *
 */
public class WMSCachedTileLoader extends TMSCachedTileLoader {

    /** limit of concurrent connections to WMS tile source (per source) */
    public static IntegerProperty THREAD_LIMIT = new IntegerProperty("imagery.wms.simultaneousConnections", 3);

    /**
     * Creates a TileLoader with separate WMS downloader.
     *
     * @param listener that will be notified when tile is loaded
     * @param name name of the cache region
     * @param connectTimeout to tile source
     * @param readTimeout from tile source
     * @param headers to be sent with requests
     * @param cacheDir place to store the cache
     * @throws IOException when there is a problem creating cache repository
     */
    public WMSCachedTileLoader(TileLoaderListener listener, String name, int connectTimeout, int readTimeout,
            Map<String, String> headers, String cacheDir) throws IOException {

        super(listener, name, connectTimeout, readTimeout, headers, cacheDir);
        setDownloadExecutor(TMSCachedTileLoader.getNewThreadPoolExecutor("WMS downloader", THREAD_LIMIT.get()));
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        return new WMSCachedTileLoaderJob(listener, tile, cache, connectTimeout, readTimeout, headers, getDownloadExecutor());
    }
}
