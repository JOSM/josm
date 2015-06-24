// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.preferences.StringProperty;

/**
 * TileLoaderFactory creating JCS cached TileLoaders
 *
 * @author Wiktor Niesiobędzki
 * @since TODO
 *
 */
public abstract class CachedTileLoaderFactory implements TileLoaderFactory {
    /**
     * Keeps the cache directory where
     */
    public static final StringProperty PROP_TILECACHE_DIR = getTileCacheDir();
    private String cacheName;

    /**
     * @param cacheName name of the cache region, that the created loader will use
     */
    public CachedTileLoaderFactory(String cacheName) {
        this.cacheName = cacheName;
    }

    private static StringProperty getTileCacheDir() {
        String defPath = null;
        try {
            defPath = new File(Main.pref.getCacheDirectory(), "tiles").getAbsolutePath();
        } catch (SecurityException e) {
            Main.warn(e);
        }
        return new StringProperty("imagery.generic.loader.cachedir", defPath);
    }

    @Override
    public TileLoader makeTileLoader(TileLoaderListener listener) {
        return makeTileLoader(listener, null);
    }

    @Override
    public TileLoader makeTileLoader(TileLoaderListener listener, Map<String, String> inputHeaders) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Version.getInstance().getFullAgentString());
        headers.put("Accept", "text/html, image/png, image/jpeg, image/gif, */*");
        if (inputHeaders != null)
            headers.putAll(inputHeaders);

        try {
            return getLoader(listener, cacheName,
                    Main.pref.getInteger("socket.timeout.connect",15) * 1000,
                    Main.pref.getInteger("socket.timeout.read", 30) * 1000,
                    headers,
                    PROP_TILECACHE_DIR.get());
        } catch (IOException e) {
            Main.warn(e);
        }
        return null;
    }

    protected abstract TileLoader getLoader(TileLoaderListener listener, String cacheName, int connectTimeout, int readTimeout, Map<String, String> headers, String cacheDir) throws IOException;
}
