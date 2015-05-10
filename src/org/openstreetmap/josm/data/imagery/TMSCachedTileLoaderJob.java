// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * @author Wiktor Niesiobędzki
 *
 * Class bridging TMS requests to JCS cache requests
 * @since 8168
 */
public class TMSCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> implements TileJob, ICachedLoaderListener  {
    private static final Logger log = FeatureAdapter.getLogger(TMSCachedTileLoaderJob.class.getCanonicalName());
    private Tile tile;
    private volatile URL url;

    // we need another deduplication of Tile Loader listeners, as for each submit, new TMSCachedTileLoaderJob was created
    // that way, we reduce calls to tileLoadingFinished, and general CPU load due to surplus Map repaints
    private static final ConcurrentMap<String,Set<TileLoaderListener>> inProgress = new ConcurrentHashMap<>();

    /**
     * Limit definition for per host concurrent connections
     */
    public static final IntegerProperty HOST_LIMIT = new IntegerProperty("imagery.tms.tmsloader.maxjobsperhost", 6);

     /*
     * Host limit guards the area - between submission to the queue up to loading is finished. It uses executionGuard method
     * from JCSCachedTileLoaderJob to acquire the semaphore, and releases it - when loadingFinished is called (but not when
     * LoadResult.GUARD_REJECTED is set)
     *
     */

    private Semaphore getSemaphore() {
        String host = getUrl().getHost();
        Semaphore limit = HOST_LIMITS.get(host);
        if (limit == null) {
            synchronized(HOST_LIMITS) {
                limit = HOST_LIMITS.get(host);
                if (limit == null) {
                    limit = new Semaphore(HOST_LIMIT.get().intValue());
                    HOST_LIMITS.put(host, limit);
                }
            }
        }
        return limit;
    }

    private boolean acquireSemaphore() {
        boolean ret = true;
        Semaphore limit = getSemaphore();
        if (limit != null) {
            ret = limit.tryAcquire();
            if (!ret) {
                Main.debug("rejecting job because of per host limit");
            }
        }
        return ret;
    }

    private void releaseSemaphore() {
        Semaphore limit = getSemaphore();
        if (limit != null) {
            limit.release();
        }
    }

    private static Map<String, Semaphore> HOST_LIMITS = new ConcurrentHashMap<>();

    /**
     * overrides the THREAD_LIMIT in superclass, as we want to have separate limit and pool for TMS
     */
    public static final IntegerProperty THREAD_LIMIT = new IntegerProperty("imagery.tms.tmsloader.maxjobs", 25);

    /**
     * separate from JCS thread pool for TMS loader, so we can have different thread pools for default JCS
     * and for TMS imagery
     */
    private static ThreadPoolExecutor DOWNLOAD_JOB_DISPATCHER = getThreadPoolExecutor();

    private static ThreadPoolExecutor getThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                THREAD_LIMIT.get().intValue(), // keep the thread number constant
                THREAD_LIMIT.get().intValue(), // do not this number of threads
                30, // keepalive for thread
                TimeUnit.SECONDS,
                // make queue of LIFO type - so recently requested tiles will be loaded first (assuming that these are which user is waiting to see)
                new LIFOQueue(5)
                    /* keep the queue size fairly small, we do not want to
                     download a lot of tiles, that user is not seeing anyway */
                );
    }

    /**
     * Reconfigures download dispatcher using current values of THREAD_LIMIT and HOST_LIMIT
     */
    public static final void reconfigureDownloadDispatcher() {
        HOST_LIMITS = new ConcurrentHashMap<>();
        DOWNLOAD_JOB_DISPATCHER = getThreadPoolExecutor();
    }

    /**
     * Constructor for creating a job, to get a specific tile from cache
     * @param listener
     * @param tile to be fetched from cache
     * @param cache object
     * @param connectTimeout when connecting to remote resource
     * @param readTimeout when connecting to remote resource
     * @param headers to be sent together with request
     */
    public TMSCachedTileLoaderJob(TileLoaderListener listener, Tile tile, ICacheAccess<String, BufferedImageCacheEntry> cache, int connectTimeout, int readTimeout,
            Map<String, String> headers) {
        super(cache, connectTimeout, readTimeout, headers);
        this.tile = tile;
        if (listener != null) {
            String deduplicationKey = getCacheKey();
            synchronized (inProgress) {
                Set<TileLoaderListener> newListeners = inProgress.get(deduplicationKey);
                if (newListeners == null) {
                    newListeners = new HashSet<>();
                    inProgress.put(deduplicationKey, newListeners);
                }
                newListeners.add(listener);
            }
        }
    }

    @Override
    public Tile getTile() {
        return getCachedTile();
    }

    @Override
    public String getCacheKey() {
        if (tile != null)
            return tile.getKey();
        return null;
    }

    /*
     *  this doesn't needs to be synchronized, as it's not that costly to keep only one execution
     *  in parallel, but URL creation and Tile.getUrl() are costly and are not needed when fetching
     *  data from cache, that's why URL creation is postponed until it's needed
     *
     *  We need to have static url value for TileLoaderJob, as for some TileSources we might get different
     *  URL's each call we made (servers switching), and URL's are used below as a key for duplicate detection
     *
     */
    @Override
    public URL getUrl() {
        if (url == null) {
            try {
                synchronized (this) {
                    if (url == null)
                        url = new URL(tile.getUrl());
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS Cache - error creating URL for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
                log.log(Level.INFO, "Exception: ", e);
            }
        }
        return url;
    }

    @Override
    public boolean isObjectLoadable() {
        if (cacheData != null) {
            byte[] content = cacheData.getContent();
            try {
                return content != null  || cacheData.getImage() != null || isNoTileAtZoom();
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS - error loading from cache for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
            }
        }
        return false;
    }

    private boolean isNoTileAtZoom() {
        if (attributes == null) {
            log.warning("Cache attributes are null");
        }
        return attributes != null && attributes.isNoTileAtZoom();
    }

    @Override
    protected boolean cacheAsEmpty(Map<String, List<String>> headers, int statusCode, byte[] content) {
        if (tile.getTileSource().isNoTileAtZoom(headers, statusCode, content)) {
            attributes.setNoTileAtZoom(true);
            return true;
        }
        return false;
    }

    private boolean handleNoTileAtZoom() {
        if (isNoTileAtZoom()) {
            log.log(Level.FINE, "JCS TMS - Tile valid, but no file, as no tiles at this level {0}", tile);
            tile.setError("No tile at this zoom level");
            tile.putValue("tile-info", "no-tile");
            return true;
        }
        return false;
    }

    @Override
    protected Executor getDownloadExecutor() {
        return DOWNLOAD_JOB_DISPATCHER;
    }

    @Override
    protected boolean executionGuard() {
        return acquireSemaphore();
    }

    @Override
    protected void executionFinished() {
        releaseSemaphore();
    }

    public void submit() {
        tile.initLoading();
        super.submit(this);
    }

    @Override
    public void loadingFinished(CacheEntry object, CacheEntryAttributes attributes, LoadResult result) {
        this.attributes = attributes; // as we might get notification from other object than our selfs, pass attributes along
        Set<TileLoaderListener> listeners;
        synchronized (inProgress) {
            listeners = inProgress.remove(getCacheKey());
        }

        try {
            if(!tile.isLoaded()) { //if someone else already loaded tile, skip all the handling
                tile.finishLoading(); // whatever happened set that loading has finished
                switch(result){
                case FAILURE:
                    tile.setError("Problem loading tile");
                    // no break intentional here
                case SUCCESS:
                    handleNoTileAtZoom();
                    if (object != null) {
                        byte[] content = object.getContent();
                        if (content != null && content.length > 0) {
                            tile.loadImage(new ByteArrayInputStream(content));
                        }
                    }
                    // no break intentional here
                case REJECTED:
                    // do nothing
                }
            }

            // always check, if there is some listener interested in fact, that tile has finished loading
            if (listeners != null) { // listeners might be null, if some other thread notified already about success
                for(TileLoaderListener l: listeners) {
                    l.tileLoadingFinished(tile, result.equals(LoadResult.SUCCESS));
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "JCS TMS - error loading object for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
            tile.setError(e.getMessage());
            tile.setLoaded(false);
            if (listeners != null) { // listeners might be null, if some other thread notified already about success
                for(TileLoaderListener l: listeners) {
                    l.tileLoadingFinished(tile, false);
                }
            }
        }
    }

    /**
     * Method for getting the tile from cache only, without trying to reach remote resource
     * @return tile or null, if nothing (useful) was found in cache
     */
    public Tile getCachedTile() {
        BufferedImageCacheEntry data = get();
        if (isObjectLoadable()) {
            try {
                if (data != null && data.getImage() != null) {
                    tile.setImage(data.getImage());
                    tile.finishLoading();
                }
                if (isNoTileAtZoom()) {
                    handleNoTileAtZoom();
                    tile.finishLoading();
                }
                return tile;
            } catch (IOException e) {
                log.log(Level.WARNING, "JCS TMS - error loading object for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
                return null;
            }

        } else {
            return tile;
        }
    }

    @Override
    protected boolean handleNotFound() {
        if (tile.getSource().isNoTileAtZoom(null, 404, null)) {
            tile.setError("No tile at this zoom level");
            tile.putValue("tile-info", "no-tile");
            return true;
        }
        return false;
    }

    /**
     * For TMS use BaseURL as settings discovery, so for different paths, we will have different settings (useful for developer servers)
     *
     * @return base URL of TMS or server url as defined in super class
     */
    @Override
    protected String getServerKey() {
        TileSource ts = tile.getSource();
        if (ts instanceof AbstractTMSTileSource) {
            return ((AbstractTMSTileSource) ts).getBaseUrl();
        }
        return super.getServerKey();
    }

    @Override
    protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
        return new BufferedImageCacheEntry(content);
    }
}
