// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import java.util.concurrent.ThreadPoolExecutor;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.CachedTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.tools.CheckParameterUtil;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;

/**
 * A TileLoader class for MVT tiles
 * @author Taylor Smock
 * @since xxx
 */
public class MapboxVectorCachedTileLoader implements TileLoader, CachedTileLoader {
    protected final ICacheAccess<String, BufferedImageCacheEntry> cache;
    protected final TileLoaderListener listener;
    protected final TileJobOptions options;
    private static final IntegerProperty THREAD_LIMIT =
            new IntegerProperty("imagery.vector.mvtloader.maxjobs", TMSCachedTileLoader.THREAD_LIMIT.getDefaultValue());
    private static final ThreadPoolExecutor DEFAULT_DOWNLOAD_JOB_DISPATCHER =
            TMSCachedTileLoader.getNewThreadPoolExecutor("MVT-downloader-%d", THREAD_LIMIT.get());

    /**
     * Constructor
     * @param listener          called when tile loading has finished
     * @param cache             of the cache
     * @param options           tile job options
     */
    public MapboxVectorCachedTileLoader(TileLoaderListener listener, ICacheAccess<String, BufferedImageCacheEntry> cache,
                                        TileJobOptions options) {
        CheckParameterUtil.ensureParameterNotNull(cache, "cache");
        this.cache = cache;
        this.options = options;
        this.listener = listener;
    }

    @Override
    public void clearCache(TileSource source) {
        this.cache.remove(source.getName() + ':');
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        return new MapboxVectorCachedTileLoaderJob(
                listener,
                tile,
                cache,
                options,
                getDownloadExecutor());
    }

    @Override
    public void cancelOutstandingTasks() {
        final ThreadPoolExecutor executor = getDownloadExecutor();
        executor.getQueue().stream().filter(executor::remove).filter(MapboxVectorCachedTileLoaderJob.class::isInstance)
                .map(MapboxVectorCachedTileLoaderJob.class::cast).forEach(JCSCachedTileLoaderJob::handleJobCancellation);
    }

    @Override
    public boolean hasOutstandingTasks() {
        return getDownloadExecutor().getTaskCount() > getDownloadExecutor().getCompletedTaskCount();
    }

    private static ThreadPoolExecutor getDownloadExecutor() {
        return DEFAULT_DOWNLOAD_JOB_DISPATCHER;
    }
}
