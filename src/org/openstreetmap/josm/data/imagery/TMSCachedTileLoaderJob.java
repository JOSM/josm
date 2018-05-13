// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileJob;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;
import org.openstreetmap.josm.data.preferences.LongProperty;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Class bridging TMS requests to JCS cache requests
 *
 * @author Wiktor Niesiobędzki
 * @since 8168
 */
public class TMSCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> implements TileJob, ICachedLoaderListener {
    /** General maximum expires for tiles. Might be overridden by imagery settings */
    public static final LongProperty MAXIMUM_EXPIRES = new LongProperty("imagery.generic.maximum_expires", TimeUnit.DAYS.toMillis(30));
    /** General minimum expires for tiles. Might be overridden by imagery settings */
    public static final LongProperty MINIMUM_EXPIRES = new LongProperty("imagery.generic.minimum_expires", TimeUnit.HOURS.toMillis(1));
    static final Pattern SERVICE_EXCEPTION_PATTERN = Pattern.compile("(?s).+<ServiceException[^>]*>(.+)</ServiceException>.+");
    protected final Tile tile;
    private volatile URL url;
    private final TileJobOptions options;

    // we need another deduplication of Tile Loader listeners, as for each submit, new TMSCachedTileLoaderJob was created
    // that way, we reduce calls to tileLoadingFinished, and general CPU load due to surplus Map repaints
    private static final ConcurrentMap<String, Set<TileLoaderListener>> inProgress = new ConcurrentHashMap<>();

    /**
     * Constructor for creating a job, to get a specific tile from cache
     * @param listener Tile loader listener
     * @param tile to be fetched from cache
     * @param cache object
     * @param options for job (such as http headers, timeouts etc.)
     * @param downloadExecutor that will be executing the jobs
     */
    @SuppressFBWarnings(value = "JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    public TMSCachedTileLoaderJob(TileLoaderListener listener, Tile tile,
            ICacheAccess<String, BufferedImageCacheEntry> cache,
            TileJobOptions options,
            ThreadPoolExecutor downloadExecutor) {
        super(cache, options, downloadExecutor);
        this.tile = tile;
        this.options = options;
        if (listener != null) {
            String deduplicationKey = getCacheKey();
            synchronized (inProgress) {
                inProgress.computeIfAbsent(deduplicationKey, k -> new HashSet<>()).add(listener);
            }
        }
    }

    @Override
    public String getCacheKey() {
        if (tile != null) {
            TileSource tileSource = tile.getTileSource();
            return Optional.ofNullable(tileSource.getName()).orElse("").replace(':', '_') + ':'
                    + tileSource.getTileId(tile.getZoom(), tile.getXtile(), tile.getYtile());
        }
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
    public URL getUrl() throws IOException {
        if (url == null) {
            synchronized (this) {
                if (url == null) {
                    String sUrl = tile.getUrl();
                    if (!"".equals(sUrl)) {
                        url = new URL(sUrl);
                    }
                }
            }
        }
        return url;
    }

    @Override
    public boolean isObjectLoadable() {
        if (cacheData != null) {
            byte[] content = cacheData.getContent();
            try {
                return content.length > 0 || cacheData.getImage() != null || isNoTileAtZoom();
            } catch (IOException e) {
                Logging.logWithStackTrace(Logging.LEVEL_WARN, e, "JCS TMS - error loading from cache for tile {0}: {1}",
                        new Object[] {tile.getKey(), e.getMessage()}
                        );
            }
        }
        return false;
    }

    @Override
    protected boolean isResponseLoadable(Map<String, List<String>> headers, int statusCode, byte[] content) {
        attributes.setMetadata(tile.getTileSource().getMetadata(headers));
        if (tile.getTileSource().isNoTileAtZoom(headers, statusCode, content)) {
            attributes.setNoTileAtZoom(true);
            return false; // do no try to load data from no-tile at zoom, cache empty object instead
        }
        return super.isResponseLoadable(headers, statusCode, content);
    }

    @Override
    protected boolean cacheAsEmpty() {
        return isNoTileAtZoom() || super.cacheAsEmpty();
    }

    @Override
    public void submit(boolean force) {
        tile.initLoading();
        try {
            super.submit(this, force);
        } catch (IOException | IllegalArgumentException e) {
            // if we fail to submit the job, mark tile as loaded and set error message
            Logging.log(Logging.LEVEL_WARN, e);
            tile.finishLoading();
            tile.setError(e.getMessage());
        }
    }

    @Override
    public void loadingFinished(CacheEntry object, CacheEntryAttributes attributes, LoadResult result) {
        this.attributes = attributes; // as we might get notification from other object than our selfs, pass attributes along
        Set<TileLoaderListener> listeners;
        synchronized (inProgress) {
            listeners = inProgress.remove(getCacheKey());
        }
        boolean status = result.equals(LoadResult.SUCCESS);

        try {
            tile.finishLoading(); // whatever happened set that loading has finished
            // set tile metadata
            if (this.attributes != null) {
                for (Entry<String, String> e: this.attributes.getMetadata().entrySet()) {
                    tile.putValue(e.getKey(), e.getValue());
                }
            }

            switch(result) {
            case SUCCESS:
                handleNoTileAtZoom();
                if (attributes != null) {
                    int httpStatusCode = attributes.getResponseCode();
                    if (httpStatusCode >= 400 && !isNoTileAtZoom()) {
                        if (attributes.getErrorMessage() == null) {
                            tile.setError(tr("HTTP error {0} when loading tiles", httpStatusCode));
                        } else {
                            tile.setError(tr("Error downloading tiles: {0}", attributes.getErrorMessage()));
                        }
                        status = false;
                    }
                }
                status &= tryLoadTileImage(object); //try to keep returned image as background
                break;
            case FAILURE:
                tile.setError("Problem loading tile");
                tryLoadTileImage(object);
                break;
            case CANCELED:
                tile.loadingCanceled();
                // do nothing
            }

            // always check, if there is some listener interested in fact, that tile has finished loading
            if (listeners != null) { // listeners might be null, if some other thread notified already about success
                for (TileLoaderListener l: listeners) {
                    l.tileLoadingFinished(tile, status);
                }
            }
        } catch (IOException e) {
            Logging.warn("JCS TMS - error loading object for tile {0}: {1}", new Object[] {tile.getKey(), e.getMessage()});
            tile.setError(e);
            tile.setLoaded(false);
            if (listeners != null) { // listeners might be null, if some other thread notified already about success
                for (TileLoaderListener l: listeners) {
                    l.tileLoadingFinished(tile, false);
                }
            }
        }
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

    @Override
    public void submit() {
        submit(false);
    }

    @Override
    protected CacheEntryAttributes parseHeaders(HttpClient.Response urlConn) {
        CacheEntryAttributes ret = super.parseHeaders(urlConn);
        // keep the expiration time between MINIMUM_EXPIRES and MAXIMUM_EXPIRES, so we will cache the tiles
        // at least for some short period of time, but not too long
        if (ret.getExpirationTime() < now + Math.max(MINIMUM_EXPIRES.get(), options.getMinimumExpiryTime())) {
            ret.setExpirationTime(now + MINIMUM_EXPIRES.get());
        }
        if (ret.getExpirationTime() > now + Math.max(MAXIMUM_EXPIRES.get(), options.getMinimumExpiryTime())) {
            ret.setExpirationTime(now + MAXIMUM_EXPIRES.get());
        }
        return ret;
    }

    private boolean handleNoTileAtZoom() {
        if (isNoTileAtZoom()) {
            Logging.debug("JCS TMS - Tile valid, but no file, as no tiles at this level {0}", tile);
            tile.setError("No tile at this zoom level");
            tile.putValue("tile-info", "no-tile");
            return true;
        }
        return false;
    }

    private boolean isNoTileAtZoom() {
        if (attributes == null) {
            Logging.warn("Cache attributes are null");
        }
        return attributes != null && attributes.isNoTileAtZoom();
    }

    private boolean tryLoadTileImage(CacheEntry object) throws IOException {
        if (object != null) {
            byte[] content = object.getContent();
            if (content.length > 0) {
                try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
                    tile.loadImage(in);
                    if (tile.getImage() == null) {
                        String s = new String(content, StandardCharsets.UTF_8);
                        Matcher m = SERVICE_EXCEPTION_PATTERN.matcher(s);
                        if (m.matches()) {
                            String message = Utils.strip(m.group(1));
                            tile.setError(message);
                            Logging.error(message);
                            Logging.debug(s);
                        } else {
                            tile.setError(tr("Could not load image from tile server"));
                        }
                        return false;
                    }
                } catch (UnsatisfiedLinkError | SecurityException e) {
                    throw new IOException(e);
                }
            }
        }
        return true;
    }
}
