// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

public interface ICachedLoaderListener {
    /**
     * Will be called when K object was successfully downloaded
     * 
     * @param data
     * @param success
     */
    public void loadingFinished(CacheEntry data, boolean success);

}
