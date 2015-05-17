// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener.LoadResult;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * @author Wiktor Niesiobędzki
 *
 * @param <K> cache entry key type
 *
 * Generic loader for HTTP based tiles. Uses custom attribute, to check, if entry has expired
 * according to HTTP headers sent with tile. If so, it tries to verify using Etags
 * or If-Modified-Since / Last-Modified.
 *
 * If the tile is not valid, it will try to download it from remote service and put it
 * to cache. If remote server will fail it will try to use stale entry.
 *
 * This class will keep only one Job running for specified tile. All others will just finish, but
 * listeners will be gathered and notified, once download job will be finished
 *
 * @since 8168
 */
public abstract class JCSCachedTileLoaderJob<K, V extends CacheEntry> implements ICachedLoaderJob<K>, Runnable {
    private static final Logger log = FeatureAdapter.getLogger(JCSCachedTileLoaderJob.class.getCanonicalName());
    protected static final long DEFAULT_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7; // 7 days
    // Limit for the max-age value send by the server.
    protected static final long EXPIRE_TIME_SERVER_LIMIT = 1000L * 60 * 60 * 24 * 28; // 4 weeks
    // Absolute expire time limit. Cached tiles that are older will not be used,
    // even if the refresh from the server fails.
    protected static final long ABSOLUTE_EXPIRE_TIME_LIMIT = Long.MAX_VALUE; // unlimited

    /**
     * maximum download threads that will be started
     */
    public static final IntegerProperty THREAD_LIMIT = new IntegerProperty("cache.jcs.max_threads", 10);

    public static class LIFOQueue extends LinkedBlockingDeque<Runnable> {
        public LIFOQueue(int capacity) {
            super(capacity);
        }

        @Override
        public boolean offer(Runnable t) {
            return super.offerFirst(t);
        }

        @Override
        public Runnable remove() {
            return super.removeFirst();
        }
    }


    /*
     * ThreadPoolExecutor starts new threads, until THREAD_LIMIT is reached. Then it puts tasks into LIFOQueue, which is fairly
     * small, but we do not want a lot of outstanding tasks queued, but rather prefer the class consumer to resubmit the task, which are
     * important right now.
     *
     * This way, if some task gets outdated (for example - user paned the map, and we do not want to download this tile any more),
     * the task will not be resubmitted, and thus - never queued.
     *
     * There is no point in canceling tasks, that are already taken by worker threads (if we made so much effort, we can at least cache
     * the response, so later it could be used). We could actually cancel what is in LIFOQueue, but this is a tradeoff between simplicity
     * and performance (we do want to have something to offer to worker threads before tasks will be resubmitted by class consumer)
     */
    private static Executor DOWNLOAD_JOB_DISPATCHER = new ThreadPoolExecutor(
            2, // we have a small queue, so threads will be quickly started (threads are started only, when queue is full)
            THREAD_LIMIT.get().intValue(), // do not this number of threads
            30, // keepalive for thread
            TimeUnit.SECONDS,
            // make queue of LIFO type - so recently requested tiles will be loaded first (assuming that these are which user is waiting to see)
            new LIFOQueue(5)
            );

    private static ConcurrentMap<String,Set<ICachedLoaderListener>> inProgress = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Boolean> useHead = new ConcurrentHashMap<>();

    private long now; // when the job started

    private ICacheAccess<K, V> cache;
    private ICacheElement<K, V> cacheElement;
    protected V cacheData = null;
    protected CacheEntryAttributes attributes = null;

    // HTTP connection parameters
    private int connectTimeout;
    private int readTimeout;
    private Map<String, String> headers;

    /**
     * @param cache cache instance that we will work on
     * @param headers
     * @param readTimeout
     * @param connectTimeout
     */
    public JCSCachedTileLoaderJob(ICacheAccess<K,V> cache,
            int connectTimeout, int readTimeout,
            Map<String, String> headers) {

        this.cache = cache;
        this.now = System.currentTimeMillis();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.headers = headers;
    }

    private void ensureCacheElement() {
        if (cacheElement == null && getCacheKey() != null) {
            cacheElement = cache.getCacheElement(getCacheKey());
            if (cacheElement != null) {
                attributes = (CacheEntryAttributes) cacheElement.getElementAttributes();
                cacheData = cacheElement.getVal();
            }
        }
    }

    public V get() {
        ensureCacheElement();
        return cacheData;
    }

    @Override
    public void submit(ICachedLoaderListener listener) {
        boolean first = false;
        URL url = getUrl();
        String deduplicationKey = null;
        if (url != null) {
            // url might be null, for example when Bing Attribution is not loaded yet
            deduplicationKey = url.toString();
        }
        if (deduplicationKey == null) {
            log.log(Level.WARNING, "No url returned for: {0}, skipping", getCacheKey());
            return;
        }
        synchronized (inProgress) {
            Set<ICachedLoaderListener> newListeners = inProgress.get(deduplicationKey);
            if (newListeners == null) {
                newListeners = new HashSet<>();
                inProgress.put(deduplicationKey, newListeners);
                first = true;
            }
            newListeners.add(listener);
        }

        if (first) {
            ensureCacheElement();
            if (cacheElement != null && isCacheElementValid() && (isObjectLoadable())) {
                // we got something in cache, and it's valid, so lets return it
                log.log(Level.FINE, "JCS - Returning object from cache: {0}", getCacheKey());
                finishLoading(LoadResult.SUCCESS);
                return;
            }
            // object not in cache, so submit work to separate thread
            try {
                if (executionGuard()) {
                    // use getter method, so subclasses may override executors, to get separate thread pool
                    getDownloadExecutor().execute(this);
                } else {
                    log.log(Level.FINE, "JCS - guard rejected job for: {0}", getCacheKey());
                    finishLoading(LoadResult.REJECTED);
                }
            } catch (RejectedExecutionException e) {
                // queue was full, try again later
                log.log(Level.FINE, "JCS - rejected job for: {0}", getCacheKey());
                finishLoading(LoadResult.REJECTED);
            }
        }
    }

    /**
     * Guard method for execution. If guard returns true, the execution of download task will commence
     * otherwise, execution will finish with result LoadResult.REJECTED
     *
     * It is responsibility of the overriding class, to handle properly situation in finishLoading class
     * @return
     */
    protected boolean executionGuard() {
        return true;
    }

    /**
     * This method is run when job has finished
     */
    protected void executionFinished() {
    }

    /**
     *
     * @return checks if object from cache has sufficient data to be returned
     */
    protected boolean isObjectLoadable() {
        byte[] content = cacheData.getContent();
        return content != null && content.length > 0;
    }

    /**
     *
     * @return cache object as empty, regardless of what remote resource has returned (ex. based on headers)
     */
    protected boolean cacheAsEmpty(Map<String, List<String>> headers, int statusCode, byte[] content) {
        return false;
    }

    /**
     * @return key under which discovered server settings will be kept
     */
    protected String getServerKey() {
        return getUrl().getHost();
    }

    /**
     * this needs to be non-static, so it can be overridden by subclasses
     */
    protected Executor getDownloadExecutor() {
        return DOWNLOAD_JOB_DISPATCHER;
    }


    public void run() {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("JCS Downloading: " + getUrl());
        try {
            // try to load object from remote resource
            if (loadObject()) {
                finishLoading(LoadResult.SUCCESS);
            } else {
                // if loading failed - check if we can return stale entry
                if (isObjectLoadable()) {
                    // try to get stale entry in cache
                    finishLoading(LoadResult.SUCCESS);
                    log.log(Level.FINE, "JCS - found stale object in cache: {0}", getUrl());
                } else {
                    // failed completely
                    finishLoading(LoadResult.FAILURE);
                }
            }
        } finally {
            executionFinished();
            currentThread.setName(oldName);
        }
    }


    private void finishLoading(LoadResult result) {
        Set<ICachedLoaderListener> listeners = null;
        synchronized (inProgress) {
            listeners = inProgress.remove(getUrl().toString());
        }
        if (listeners == null) {
            log.log(Level.WARNING, "Listener not found for URL: {0}. Listener not notified!", getUrl());
            return;
        }
        try {
            for (ICachedLoaderListener l: listeners) {
                l.loadingFinished(cacheData, attributes, result);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "JCS - Error while loading object from cache: {0}; {1}", new Object[]{e.getMessage(), getUrl()});
            Main.warn(e);
            for (ICachedLoaderListener l: listeners) {
                l.loadingFinished(cacheData, attributes, LoadResult.FAILURE);
            }

        }

    }

    private boolean isCacheElementValid() {
        long expires = attributes.getExpirationTime();

        // check by expire date set by server
        if (expires != 0L) {
            // put a limit to the expire time (some servers send a value
            // that is too large)
            expires = Math.min(expires, attributes.getCreateTime() + EXPIRE_TIME_SERVER_LIMIT);
            if (now > expires) {
                log.log(Level.FINE, "JCS - Object {0} has expired -> valid to {1}, now is: {2}", new Object[]{getUrl(), Long.toString(expires), Long.toString(now)});
                return false;
            }
        } else {
            // check by file modification date
            if (now - attributes.getLastModification() > DEFAULT_EXPIRE_TIME) {
                log.log(Level.FINE, "JCS - Object has expired, maximum file age reached {0}", getUrl());
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if object was successfully downloaded, false, if there was a loading failure
     */

    private boolean loadObject() {
        try {
            // if we have object in cache, and host doesn't support If-Modified-Since nor If-None-Match
            // then just use HEAD request and check returned values
            if (isObjectLoadable() &&
                    Boolean.TRUE.equals(useHead.get(getServerKey())) &&
                    isCacheValidUsingHead()) {
                log.log(Level.FINE, "JCS - cache entry verified using HEAD request: {0}", getUrl());
                return true;
            }
            HttpURLConnection urlConn = getURLConnection();

            if (isObjectLoadable()  &&
                    (now - attributes.getLastModification()) <= ABSOLUTE_EXPIRE_TIME_LIMIT) {
                urlConn.setIfModifiedSince(attributes.getLastModification());
            }
            if (isObjectLoadable() && attributes.getEtag() != null) {
                urlConn.addRequestProperty("If-None-Match", attributes.getEtag());
            }
            if (urlConn.getResponseCode() == 304) {
                // If isModifiedSince or If-None-Match has been set
                // and the server answers with a HTTP 304 = "Not Modified"
                log.log(Level.FINE, "JCS - IfModifiedSince/Etag test: local version is up to date: {0}", getUrl());
                return true;
            } else if (isObjectLoadable()) {
                // we have an object in cache, but we haven't received 304 resposne code
                // check if we should use HEAD request to verify
                if((attributes.getEtag() != null && attributes.getEtag().equals(urlConn.getRequestProperty("ETag"))) ||
                        attributes.getLastModification() == urlConn.getLastModified()) {
                    // we sent ETag or If-Modified-Since, but didn't get 304 response code
                    // for further requests - use HEAD
                    String serverKey = getServerKey();
                    log.log(Level.INFO, "JCS - Host: {0} found not to return 304 codes for If-Modifed-Since or If-None-Match headers", serverKey);
                    useHead.put(serverKey, Boolean.TRUE);
                }
            }

            attributes = parseHeaders(urlConn);

            for (int i = 0; i < 5; ++i) {
                if (urlConn.getResponseCode() == 503) {
                    Thread.sleep(5000+(new Random()).nextInt(5000));
                    continue;
                }

                attributes.setResponseCode(urlConn.getResponseCode());
                byte[] raw = read(urlConn);

                if (!cacheAsEmpty(urlConn.getHeaderFields(), urlConn.getResponseCode(), raw) &&
                        raw != null && raw.length > 0) {
                    // we need to check cacheEmpty, so for cases, when data is returned, but we want to store
                    // as empty (eg. empty tile images) to save some space
                    cacheData = createCacheEntry(raw);
                    cache.put(getCacheKey(), cacheData, attributes);
                    log.log(Level.FINE, "JCS - downloaded key: {0}, length: {1}, url: {2}",
                            new Object[] {getCacheKey(), raw.length, getUrl()});
                    return true;
                } else  {
                    cacheData = createCacheEntry(new byte[]{});
                    cache.put(getCacheKey(), cacheData, attributes);
                    log.log(Level.FINE, "JCS - Caching empty object {0}", getUrl());
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            log.log(Level.FINE, "JCS - Caching empty object as server returned 404 for: {0}", getUrl());
            cache.put(getCacheKey(), createCacheEntry(new byte[]{}), attributes);
            return handleNotFound();
        } catch (Exception e) {
            log.log(Level.WARNING, "JCS - Exception during download {0}",  getUrl());
            Main.warn(e);
        }
        log.log(Level.WARNING, "JCS - Silent failure during download: {0}", getUrl());
        return false;

    }

    /**
     *  @return if we should treat this object as properly loaded
     */
    protected abstract boolean handleNotFound();

    protected abstract V createCacheEntry(byte[] content);

    private CacheEntryAttributes parseHeaders(URLConnection urlConn) {
        CacheEntryAttributes ret = new CacheEntryAttributes();

        Long lng = urlConn.getExpiration();
        if (lng.equals(0L)) {
            try {
                String str = urlConn.getHeaderField("Cache-Control");
                if (str != null) {
                    for (String token: str.split(",")) {
                        if (token.startsWith("max-age=")) {
                            lng = Long.parseLong(token.substring(8)) * 1000 +
                                    System.currentTimeMillis();
                        }
                    }
                }
            } catch (NumberFormatException e) {} //ignore malformed Cache-Control headers
        }

        ret.setExpirationTime(lng);
        ret.setLastModification(now);
        ret.setEtag(urlConn.getHeaderField("ETag"));
        return ret;
    }

    private HttpURLConnection getURLConnection() throws IOException {
        HttpURLConnection urlConn = (HttpURLConnection) getUrl().openConnection();
        urlConn.setRequestProperty("Accept", "text/html, image/png, image/jpeg, image/gif, */*");
        urlConn.setReadTimeout(readTimeout); // 30 seconds read timeout
        urlConn.setConnectTimeout(connectTimeout);
        for(Map.Entry<String, String> e: headers.entrySet()) {
            urlConn.setRequestProperty(e.getKey(), e.getValue());
        }
        return urlConn;
    }

    private boolean isCacheValidUsingHead() throws IOException {
        HttpURLConnection urlConn = (HttpURLConnection) getUrl().openConnection();
        urlConn.setRequestMethod("HEAD");
        long lastModified = urlConn.getLastModified();
        return (attributes.getEtag() != null && attributes.getEtag().equals(urlConn.getRequestProperty("ETag"))) ||
               (lastModified != 0 && lastModified <= attributes.getLastModification());
    }

    private static byte[] read(URLConnection urlConn) throws IOException {
        InputStream input = urlConn.getInputStream();
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(input.available());
            byte[] buffer = new byte[2048];
            boolean finished = false;
            do {
                int read = input.read(buffer);
                if (read >= 0) {
                    bout.write(buffer, 0, read);
                } else {
                    finished = true;
                }
            } while (!finished);
            if (bout.size() == 0)
                return null;
            return bout.toByteArray();
        } finally {
            input.close();
        }
    }
}
