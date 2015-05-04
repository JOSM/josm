// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.CachedTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileCache;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * @author Wiktor Niesiobędzki
 *
 * Wrapper class that bridges between JCS cache and Tile Loaders
 *
 */
public class TMSCachedTileLoader implements TileLoader, CachedTileLoader, TileCache {

    private ICacheAccess<String, BufferedImageCacheEntry> cache;
    private int connectTimeout;
    private int readTimeout;
    private Map<String, String> headers;
    private TileLoaderListener listener;
    public static final String PREFERENCE_PREFIX   = "imagery.tms.cache.";
    // average tile size is about 20kb
    public static final IntegerProperty MAX_OBJECTS_ON_DISK = new IntegerProperty(PREFERENCE_PREFIX + "max_objects_disk", 25000); // 25000 is around 500MB under this assumptions


    /**
     * Constructor
     * @param listener          called when tile loading has finished
     * @param name              of the cache
     * @param connectTimeout    to remote resource
     * @param readTimeout       to remote resource
     * @param headers           to be sent along with request
     * @param cacheDir          where cache file shall reside
     * @throws IOException      when cache initialization fails
     */
    public TMSCachedTileLoader(TileLoaderListener listener, String name, int connectTimeout, int readTimeout, Map<String, String> headers, String cacheDir) throws IOException {
        this.cache = JCSCacheManager.getCache(name,
                200, // use fairly small memory cache, as cached objects are quite big, as they contain BufferedImages
                MAX_OBJECTS_ON_DISK.get(),
                cacheDir);
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        return new TMSCachedTileLoaderJob(listener, tile, cache, connectTimeout, readTimeout, headers);
    }

    @Override
    public void clearCache(TileSource source) {
        this.cache.clear();
    }

    @Override
    public Tile getTile(TileSource source, int x, int y, int z) {
        return createTileLoaderJob(new Tile(source,x, y, z)).getTile();
    }

    @Override
    public void addTile(Tile tile) {
        createTileLoaderJob(tile).getTile();
    }

    @Override
    public int getTileCount() {
        return 0;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    public String getStats() {
        return cache.getStats();
    }
}
