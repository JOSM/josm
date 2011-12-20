// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * MultiMap - maps keys to multiple values
 *
 * Corresponds to Google guava LinkedHashMultimap and Apache Collections MultiValueMap
 * but it is an independent (simple) implementation.
 *
 */
public class MultiMap<A, B> {

    private final Map<A, LinkedHashSet<B>> map;

    public MultiMap() {
        map = new HashMap<A, LinkedHashSet<B>>();
    }

    public MultiMap(int capacity) {
        map = new HashMap<A, LinkedHashSet<B>>(capacity);
    }

    /**
     * Map a key to a value. Can be called multiple times with the same key, but different value.
     */
    public void put(A key, B value) {
        LinkedHashSet<B> vals = map.get(key);
        if (vals == null) {
            vals = new LinkedHashSet<B>();
            map.put(key, vals);
        }
        vals.add(value);
    }

    /**
     * Put a key that maps to nothing. (Only if it is not already in the map)
     * Afterwards containsKey(key) will return true and get(key) will return
     * an empty Set instead of null.
     */
    public void putVoid(A key) {
        if (map.containsKey(key))
            return;
        map.put(key, new LinkedHashSet<B>());
    }

    /**
     * Get the keySet
     */
    public Set<A> keySet() {
        return map.keySet();
    }

    /**
     * Return the Set associated with the given key. Result is null if
     * nothing has been mapped to this key. Modifications of the returned list
     * changes the underling map, but you should better not do that.
     */
    public Set<B> get(A key) {
        return map.get(key);
    }

    /**
     * Like get, but returns an empty Set if nothing has been mapped to the key.
     */
    public LinkedHashSet<B> getValues(A key) {
        if (!map.containsKey(key))
            return new LinkedHashSet<B>();
        return map.get(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(A key) {
        return map.containsKey(key);
    }

    /**
     * Returns true if the multimap contains a value for a key
     * @param key The key
     * @param value The value
     * @return true if the key contains the value
     */
    public boolean contains(A key, B value) {
        Set<B> values = get(key);
        return (values == null) ? false : values.contains(value);
    }

    public void clear() {
        map.clear();
    }

    public Set<Entry<A, LinkedHashSet<B>>> entrySet() {
        return map.entrySet();
    }

    /**
     * number of keys
     */
    public int size() {
        return map.size();
    }

    /**
     * returns a collection of all value sets
     */
    public Collection<LinkedHashSet<B>> values() {
        return map.values();
    }

    /**
     * Removes a cerain key=value mapping
     *
     * @return true, if something was removed
     */
    public boolean remove(A key, B value) {
        Set<B> values = get(key);
        if (values != null) {
            return values.remove(value);
        }
        return false;
    }

    /**
     * Removes all mappings for a certain key
     */
    public LinkedHashSet<B> remove(A key) {
        return map.remove(key);
    }

    public String toString() {
        List<String> entries = new ArrayList<String>(map.size());
        for (A key : map.keySet()) {
            entries.add(key + "->{" + Utils.join(",", map.get(key)) + "}");
        }
        return "(" + Utils.join(",", entries) + ")";
    }
}
