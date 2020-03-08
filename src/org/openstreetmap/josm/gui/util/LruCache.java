// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache
 * @see <a href="http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html">
 *     Java Planet: How to set up a simple LRU cache using LinkedHashMap</a>
 */
public final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private final int capacity;

    public LruCache(int capacity) {
        super(capacity + 1, 1.1f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
