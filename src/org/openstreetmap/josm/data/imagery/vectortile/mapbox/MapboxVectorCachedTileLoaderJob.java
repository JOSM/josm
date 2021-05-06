// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import java.util.concurrent.ThreadPoolExecutor;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TileJobOptions;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;

/**
 * Bridge to JCS cache for MVT tiles
 * @author Taylor Smock
 * @since 17862
 */
public class MapboxVectorCachedTileLoaderJob extends TMSCachedTileLoaderJob {

    public MapboxVectorCachedTileLoaderJob(TileLoaderListener listener, Tile tile,
                                           ICacheAccess<String, BufferedImageCacheEntry> cache, TileJobOptions options,
                                           ThreadPoolExecutor downloadExecutor) {
        super(listener, tile, cache, options, downloadExecutor);
    }
}
