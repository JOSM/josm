// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.CachedTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.HostLimitQueue;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Wrapper class that bridges between JCS cache and Tile Loaders
 *
 * @author Wiktor Niesiobędzki
 */
public class TMSCachedTileLoader implements TileLoader, CachedTileLoader {

    protected final ICacheAccess<String, BufferedImageCacheEntry> cache;
    protected final TileLoaderListener listener;

    /**
     * overrides the THREAD_LIMIT in superclass, as we want to have separate limit and pool for TMS
     */

    public static final IntegerProperty THREAD_LIMIT = new IntegerProperty("imagery.tms.tmsloader.maxjobs", 25);

    /**
     * Limit definition for per host concurrent connections
     */
    public static final IntegerProperty HOST_LIMIT = new IntegerProperty("imagery.tms.tmsloader.maxjobsperhost", 6);

    /**
     * separate from JCS thread pool for TMS loader, so we can have different thread pools for default JCS
     * and for TMS imagery
     */
    private static final ThreadPoolExecutor DEFAULT_DOWNLOAD_JOB_DISPATCHER = getNewThreadPoolExecutor("TMS-downloader-%d");

    private ThreadPoolExecutor downloadExecutor = DEFAULT_DOWNLOAD_JOB_DISPATCHER;
    protected final TileJobOptions options;

    /**
     * Constructor
     * @param listener          called when tile loading has finished
     * @param cache             of the cache
     * @param options           tile job options
     */
    public TMSCachedTileLoader(TileLoaderListener listener, ICacheAccess<String, BufferedImageCacheEntry> cache,
           TileJobOptions options) {
        CheckParameterUtil.ensureParameterNotNull(cache, "cache");
        this.cache = cache;
        this.options = options;
        this.listener = listener;
    }

    /**
     * Returns a new {@link ThreadPoolExecutor}.
     * @param nameFormat see {@link Utils#newThreadFactory(String, int)}
     * @param workers number of worker thread to keep
     * @return new ThreadPoolExecutor that will use a @see HostLimitQueue based queue
     */
    public static ThreadPoolExecutor getNewThreadPoolExecutor(String nameFormat, int workers) {
        return getNewThreadPoolExecutor(nameFormat, workers, HOST_LIMIT.get().intValue());
    }

    /**
     * Returns a new {@link ThreadPoolExecutor}.
     * @param nameFormat see {@link Utils#newThreadFactory(String, int)}
     * @param workers number of worker thread to keep
     * @param hostLimit number of concurrent downloads per host allowed
     * @return new ThreadPoolExecutor that will use a @see HostLimitQueue based queue
     */
    public static ThreadPoolExecutor getNewThreadPoolExecutor(String nameFormat, int workers, int hostLimit) {
        return new ThreadPoolExecutor(
                workers, // keep core pool the same size as max, as we use unbounded queue so there will
                workers, // be never more threads than corePoolSize
                300, // keep alive for thread
                TimeUnit.SECONDS,
                new HostLimitQueue(hostLimit),
                Utils.newThreadFactory(nameFormat, Thread.NORM_PRIORITY)
                );
    }

    /**
     * Returns a new {@link ThreadPoolExecutor}.
     * @param name name of threads
     * @return new ThreadPoolExecutor that will use a {@link HostLimitQueue} based queue, with default number of threads
     */
    public static ThreadPoolExecutor getNewThreadPoolExecutor(String name) {
        return getNewThreadPoolExecutor(name, THREAD_LIMIT.get().intValue());
    }

    @Override
    public TileJob createTileLoaderJob(Tile tile) {
        return new TMSCachedTileLoaderJob(
                listener,
                tile,
                cache,
                options,
                getDownloadExecutor());
    }

    @Override
    public void clearCache(TileSource source) {
        this.cache.remove(source.getName() + ':');
    }

    /**
     * Returns cache statistics as string.
     * @return cache statistics as string
     */
    public String getStats() {
        return cache.getStats();
    }

    /**
     * cancels all outstanding tasks in the queue. This rollbacks the state of the tiles in the queue
     * to loading = false / loaded = false
     */
    @Override
    public void cancelOutstandingTasks() {
        for (Runnable r: downloadExecutor.getQueue()) {
            if (downloadExecutor.remove(r) && r instanceof TMSCachedTileLoaderJob) {
                ((TMSCachedTileLoaderJob) r).handleJobCancellation();
            }
        }
    }

    @Override
    public boolean hasOutstandingTasks() {
        return downloadExecutor.getTaskCount() > downloadExecutor.getCompletedTaskCount();
    }

    /**
     * Sets the download executor that will be used to download tiles instead of default one.
     * You can use {@link #getNewThreadPoolExecutor} to create a new download executor with separate
     * queue from default.
     *
     * @param downloadExecutor download executor that will be used to download tiles
     */
    public void setDownloadExecutor(ThreadPoolExecutor downloadExecutor) {
        this.downloadExecutor = downloadExecutor;
    }

    /**
     * Returns download executor that is used by this factory.
     * @return download executor that is used by this factory
     */
    public ThreadPoolExecutor getDownloadExecutor() {
        return downloadExecutor;
    }
}
