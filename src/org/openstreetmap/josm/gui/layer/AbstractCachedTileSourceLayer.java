// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.imagery.CachedTileLoaderFactory;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.TileLoaderFactory;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 *
 * Class providing cache to other layers
 *
 * @author Wiktor Niesiobędzki
 *
 */
public abstract class AbstractCachedTileSourceLayer extends AbstractTileSourceLayer {
    /** loader factory responsible for loading tiles for all layers */
    private static Map<String, TileLoaderFactory> loaderFactories = new ConcurrentHashMap<>();

    private static final String PREFERENCE_PREFIX = "imagery.cache.";

    private static volatile TileLoaderFactory loaderFactoryOverride = null;

    /**
     * how many object on disk should be stored for TMS region in MB. 500 MB is default value
     */
    public static final IntegerProperty MAX_DISK_CACHE_SIZE = new IntegerProperty(PREFERENCE_PREFIX + "max_disk_size", 512);

    private ICacheAccess<String, BufferedImageCacheEntry> cache;
    private volatile TileLoaderFactory loaderFactory;


    /**
     * Creates an instance of class based on InageryInfo
     *
     * @param info ImageryInfo describing the layer
     */
    public AbstractCachedTileSourceLayer(ImageryInfo info) {
        super(info);

        if (loaderFactoryOverride != null) {
            loaderFactory = loaderFactoryOverride;
        } else {
            String key = this.getClass().getCanonicalName();
            loaderFactory = loaderFactories.get(key);
            if (loaderFactory == null) {
                synchronized (AbstractCachedTileSourceLayer.class) {
                    // check again, maybe another thread initialized factory
                    loaderFactory = loaderFactories.get(key);
                    if (loaderFactory == null) {
                        loaderFactory = new CachedTileLoaderFactory(getCache(), getTileLoaderClass());
                        loaderFactories.put(key, loaderFactory);
                    }
                }
            }
        }
    }

    @Override
    protected synchronized TileLoaderFactory getTileLoaderFactory() {
        if (loaderFactory == null) {
            loaderFactory = new CachedTileLoaderFactory(getCache(), getTileLoaderClass());
        }
        return loaderFactory;
    }

    /**
     * @return cache used by this layer
     */
    private synchronized ICacheAccess<String, BufferedImageCacheEntry> getCache() {
        if (cache != null) {
            return cache;
        }
        try {
            cache = JCSCacheManager.getCache(getCacheName(),
                    0,
                    getDiskCacheSize(),
                    CachedTileLoaderFactory.PROP_TILECACHE_DIR.get());
            return cache;
        } catch (IOException e) {
            Main.warn(e);
            return null;
        }
    }


    /**
     * Plugins that wish to set custom tile loader should call this method
     * @param newLoaderFactory that will be used to load tiles
     */

    public static synchronized void setTileLoaderFactory(TileLoaderFactory newLoaderFactory) {
        loaderFactoryOverride = newLoaderFactory;
    }

    /**
     * Returns tile loader factory for cache region and specified TileLoader class
     * @param name of the cache region
     * @param klazz type of the TileLoader
     * @return factory returning cached tile loaders using specified cache and TileLoaders
     */
    public static TileLoaderFactory getTileLoaderFactory(String name, Class<? extends TileLoader> klazz) {
        return new CachedTileLoaderFactory(getCache(name), klazz);
    }

    /**
     * @param name of cache region
     * @return cache configured object for specified cache region
     */
    public static CacheAccess<String, BufferedImageCacheEntry> getCache(String name) {
            try {
                return JCSCacheManager.getCache(name,
                        0,
                        MAX_DISK_CACHE_SIZE.get() * 1024, // MAX_DISK_CACHE_SIZE is in MB, needs to by in sync with getDiskCacheSize
                        CachedTileLoaderFactory.PROP_TILECACHE_DIR.get());
            } catch (IOException e) {
                Main.warn(e);
                return null;
            }
    }

    protected abstract Class<? extends TileLoader> getTileLoaderClass();

    protected int getDiskCacheSize() {
        return MAX_DISK_CACHE_SIZE.get() * 1024;
    }

    protected abstract String getCacheName();
}
