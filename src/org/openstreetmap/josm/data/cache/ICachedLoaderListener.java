// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

public interface ICachedLoaderListener {

    /**
     * Result of download
     *
     */
    enum LoadResult {
        SUCCESS,
        FAILURE,
        REJECTED
    }
    /**
     * Will be called when K object processed. The result might be:
     * LoadResult.SUCCESS when object was fetched
     * LoadResult.FAILURE when there was a failure during download
     * LoadResult.REJECTED when job was rejected because of full queue
     *
     * @param data
     * @param attributes
     * @param result
     */
    public void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result);

}
