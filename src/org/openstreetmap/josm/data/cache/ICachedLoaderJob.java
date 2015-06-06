// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.net.URL;

/**
 *
 * @author Wiktor Niesiobędzki
 *
 * @param <K> cache key type
 */
public interface ICachedLoaderJob<K> {
    /**
     * returns cache entry key
     *
     * @return cache key for tile
     */
    public K getCacheKey();

    /**
     * method to get download URL for Job
     * @return URL that should be fetched
     *
     */
    public URL getUrl();
    /**
     * implements the main algorithm for fetching
     */
    public void run();

    /**
     * fetches object from cache, or returns null when object is not found
     *
     * @return filled tile with data or null when no cache entry found
     */
    public CacheEntry get();

    /**
     * Submit job for background fetch, and listener will be fed with value object
     *
     * @param listener cache loader listener
     * @param force true if the load should skip all the caches (local & remote)
     */
    public void submit(ICachedLoaderListener listener, boolean force);
}
