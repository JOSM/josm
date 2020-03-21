// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LRU cache (least recently used)
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see <a href="http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html">
 *     Java Planet: How to set up a simple LRU cache using LinkedHashMap</a>
 */
public final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private final int capacity;

    /**
     * Constructs an empty {@code LruCache} instance with the given capacity
     * @param capacity the capacity
     */
    public LruCache(int capacity) {
        super(capacity + 1, 1.1f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LruCache<?, ?> lruCache = (LruCache<?, ?>) o;
        return capacity == lruCache.capacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), capacity);
    }
}
