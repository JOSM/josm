// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * TileLoaderFactory creating JCS cached TileLoaders
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class CachedTileLoaderFactory implements TileLoaderFactory {
    /**
     * Keeps the cache directory where
     */
    public static final StringProperty PROP_TILECACHE_DIR = getTileCacheDir();
    private final ICacheAccess<String, BufferedImageCacheEntry> cache;
    private Constructor<? extends TileLoader> tileLoaderConstructor;

    /**
     * @param cache cache instance which will be used by tile loaders created by this tile loader
     * @param tileLoaderClass tile loader class that will be created
     * @throws IllegalArgumentException if a suitable constructor cannot be found for {@code tileLoaderClass}
     */
    public CachedTileLoaderFactory(ICacheAccess<String, BufferedImageCacheEntry> cache, Class<? extends TileLoader> tileLoaderClass) {
        CheckParameterUtil.ensureParameterNotNull(cache, "cache");
        this.cache = cache;
        try {
            tileLoaderConstructor = tileLoaderClass.getConstructor(
                    TileLoaderListener.class,
                    ICacheAccess.class,
                    int.class,
                    int.class,
                    Map.class);
        } catch (NoSuchMethodException | SecurityException e) {
            Logging.warn(e);
            throw new IllegalArgumentException(e);
        }
    }

    private static StringProperty getTileCacheDir() {
        String defPath = null;
        try {
            defPath = new File(Config.getDirs().getCacheDirectory(), "tiles").getAbsolutePath();
        } catch (SecurityException e) {
            Logging.warn(e);
        }
        return new StringProperty("imagery.generic.loader.cachedir", defPath);
    }

    @Override
    public TileLoader makeTileLoader(TileLoaderListener listener, Map<String, String> inputHeaders) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put("User-Agent", Version.getInstance().getFullAgentString());
        headers.put("Accept", "text/html, image/png, image/jpeg, image/gif, */*");
        if (inputHeaders != null)
            headers.putAll(inputHeaders);

        return getLoader(listener, cache,
                (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.connect", 15)),
                (int) TimeUnit.SECONDS.toMillis(Config.getPref().getInt("socket.timeout.read", 30)),
                headers);
    }

    protected TileLoader getLoader(TileLoaderListener listener, ICacheAccess<String, BufferedImageCacheEntry> cache,
            int connectTimeout, int readTimeout, Map<String, String> headers) {
        try {
            return tileLoaderConstructor.newInstance(
                    listener,
                    cache,
                    connectTimeout,
                    readTimeout,
                    headers);
        } catch (IllegalArgumentException e) {
            Logging.warn(e);
            throw e;
        } catch (ReflectiveOperationException e) {
            Logging.warn(e);
            throw new IllegalArgumentException(e);
        }
    }
}
