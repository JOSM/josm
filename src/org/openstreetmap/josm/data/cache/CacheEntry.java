// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.Serializable;

/**
 * @author Wiktor Niesiobędzki
 *
 * Class that will hold JCS cache entries
 *
 */
public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L; //version
    protected byte[] content;

    /**
     * @param content of the cache entry
     */
    public CacheEntry(byte[] content) {
        this.content = content;
    }

    /**
     * @return cache entry content
     */
    public byte[] getContent() {
        return content;
    }
}
