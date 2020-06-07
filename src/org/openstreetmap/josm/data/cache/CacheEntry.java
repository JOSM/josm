// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Class that will hold JCS cache entries
 *
 * @author Wiktor Niesiobędzki
 */
public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L; //version
    protected byte[] content;

    /**
     * @param content of the cache entry
     */
    public CacheEntry(byte[] content) {
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * Returns cache entry content.
     * @return cache entry content
     */
    public byte[] getContent() {
        if (content == null) {
            return new byte[]{};
        }
        return Arrays.copyOf(content, content.length);
    }
}
