// License: GPL. For details, see LICENSE file.
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
 * MultiMap - maps keys to multiple values.
 *
 * Corresponds to Google guava LinkedHashMultimap and Apache Collections MultiValueMap
 * but it is an independent (simple) implementation.
 *
 * @param <A> Key type
 * @param <B> Value type
 *
 * @since 2702
 */
public class MultiMap<A, B> {

    private final Map<A, Set<B>> map;

    /**
     * Constructs a new {@code MultiMap}.
     */
    public MultiMap() {
        map = new HashMap<>();
    }

    /**
     * Constructs a new {@code MultiMap} with the specified initial capacity.
     * @param capacity the initial capacity
     */
    public MultiMap(int capacity) {
        map = new HashMap<>(capacity);
    }

    /**
     * Map a key to a value.
     *
     * Can be called multiple times with the same key, but different value.
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(A key, B value) {
        Set<B> vals = map.get(key);
        if (vals == null) {
            vals = new LinkedHashSet<>();
            map.put(key, vals);
        }
        vals.add(value);
    }

    /**
     * Put a key that maps to nothing. (Only if it is not already in the map)
     *
     * Afterwards containsKey(key) will return true and get(key) will return
     * an empty Set instead of null.
     * @param key key with which an empty set is to be associated
     */
    public void putVoid(A key) {
        if (map.containsKey(key))
            return;
        map.put(key, new LinkedHashSet<B>());
    }

    /**
     * Map the key to all the given values.
     *
     * Adds to the mappings that are already there.
     * @param key key with which the specified values are to be associated
     * @param values values to be associated with the specified key
     */
    public void putAll(A key, Collection<B> values) {
        Set<B> vals = map.get(key);
        if (vals == null) {
            vals = new LinkedHashSet<>(values);
            map.put(key, vals);
        }
        vals.addAll(values);
    }

    /**
     * Get the keySet.
     * @return a set view of the keys contained in this map
     * @see Map#keySet()
     */
    public Set<A> keySet() {
        return map.keySet();
    }

    /**
     * Returns the Set associated with the given key. Result is null if
     * nothing has been mapped to this key.
     *
     * Modifications of the returned list changes the underling map,
     * but you should better not do that.
     * @param key the key whose associated value is to be returned
     * @return the set of values to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
     * @see Map#get(Object)
     */
    public Set<B> get(A key) {
        return map.get(key);
    }

    /**
     * Like get, but returns an empty Set if nothing has been mapped to the key.
     * @param key the key whose associated value is to be returned
     * @return the set of values to which the specified key is mapped, or an empty set if this map contains no mapping for the key
     */
    public Set<B> getValues(A key) {
        if (!map.containsKey(key))
            return new LinkedHashSet<>();
        return map.get(key);
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * @return {@code true} if this map contains no key-value mappings
     * @see Map#isEmpty()
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @see Map#containsKey(Object)
     */
    public boolean containsKey(A key) {
        return map.containsKey(key);
    }

    /**
     * Returns true if the multimap contains a value for a key.
     *
     * @param key The key
     * @param value The value
     * @return true if the key contains the value
     */
    public boolean contains(A key, B value) {
        Set<B> values = get(key);
        return (values == null) ? false : values.contains(value);
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     * @see Map#clear()
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns a Set view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are reflected in the set, and vice-versa.
     * @return a set view of the mappings contained in this map
     * @see Map#entrySet()
     */
    public Set<Entry<A, Set<B>>> entrySet() {
        return map.entrySet();
    }

    /**
     * Returns the number of keys.
     * @return the number of key-value mappings in this map
     * @see Map#size()
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns a collection of all value sets.
     * @return a collection view of the values contained in this map
     * @see Map#values()
     */
    public Collection<Set<B>> values() {
        return map.values();
    }

    /**
     * Removes a certain key=value mapping.
     * @param key key whose mapping is to be removed from the map
     * @param value value whose mapping is to be removed from the map
     *
     * @return {@code true}, if something was removed
     */
    public boolean remove(A key, B value) {
        Set<B> values = get(key);
        if (values != null) {
            return values.remove(value);
        }
        return false;
    }

    /**
     * Removes all mappings for a certain key.
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or {@code null} if there was no mapping for key.
     * @see Map#remove(Object)
     */
    public Set<B> remove(A key) {
        return map.remove(key);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MultiMap))
            return false;
        return map.equals(((MultiMap<?,?>) obj).map);
    }

    @Override
    public String toString() {
        List<String> entries = new ArrayList<>(map.size());
        for (Entry<A, Set<B>> entry : map.entrySet()) {
            entries.add(entry.getKey() + "->{" + Utils.join(",", entry.getValue()) + "}");
        }
        return "(" + Utils.join(",", entries) + ")";
    }
}
