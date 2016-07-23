// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

/**
 * Cache loader listener.
 * @since 8168
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ICachedLoaderListener {

    /**
     * Result of download
     */
    enum LoadResult {
        SUCCESS,
        FAILURE,
        CANCELED
    }

    /**
     * Will be called when K object processed. The result might be:
     * LoadResult.SUCCESS when object was fetched
     * LoadResult.FAILURE when there was a failure during download
     * LoadResult.REJECTED when job was rejected because of full queue
     *
     * @param data cache entry contents
     * @param attributes cache entry attributes
     * @param result load result (success, failure, canceled)
     */
    void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result);
}
