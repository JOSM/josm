// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.IOException;
import java.net.URL;

/**
 * Interface for a job to load a single object (typically an imagery tile).
 * It is either retrieved from cache or downloaded from the given URL ({@link #getUrl()}).
 *
 * @author Wiktor Niesiobędzki
 *
 * @param <K> cache key type
 */
public interface ICachedLoaderJob<K> extends Runnable {
    /**
     * returns cache entry key
     *
     * @return cache key for tile
     */
    K getCacheKey();

    /**
     * method to get download URL for Job
     * @return URL that should be fetched
     * @throws IOException when could not determine the URL of the tile
     *
     */
    URL getUrl() throws IOException;

    /**
     * fetches object from cache, or returns null when object is not found
     *
     * @return filled tile with data or null when no cache entry found
     */
    CacheEntry get();

    /**
     * Submit job for background fetch, and listener will be fed with value object
     *
     * @param listener cache loader listener
     * @param force true if the load should skip all the caches (local &amp; remote)
     * @throws IOException on failure from getUrl() call
     */
    void submit(ICachedLoaderListener listener, boolean force) throws IOException;
}
