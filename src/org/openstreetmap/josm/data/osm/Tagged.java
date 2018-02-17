// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Map;

/**
 * Objects implement Tagged if they provide a map of key/value pairs.
 *
 * @since 2115
 */
// FIXME: better naming? setTags(), getTags(), getKeys() instead of keySet() ?
//
public interface Tagged {

    /**
     * The maximum tag length allowed by OSM API
     * @since 13414
     */
    int MAX_TAG_LENGTH = 255;

    /**
     * Sets the map of key/value pairs
     *
     * @param keys the map of key value pairs. If null, reset to the empty map.
     */
    void setKeys(Map<String, String> keys);

    /**
     * Replies the map of key/value pairs. Never null, but may be the empty map.
     *
     * @return the map of key/value pairs
     */
    Map<String, String> getKeys();

    /**
     * Sets a key/value pairs
     *
     * @param key the key
     * @param value the value. If null, removes the key/value pair.
     */
    void put(String key, String value);

    /**
     * Sets a key/value pairs
     *
     * @param tag The tag to set.
     * @since 10736
     */
    default void put(Tag tag) {
        put(tag.getKey(), tag.getValue());
    }

    /**
     * Replies the value of the given key; null, if there is no value for this key
     *
     * @param key the key
     * @return the value
     */
    String get(String key);

    /**
     * Removes a given key/value pair
     *
     * @param key the key
     */
    void remove(String key);

    /**
     * Replies true, if there is at least one key/value pair; false, otherwise
     *
     * @return true, if there is at least one key/value pair; false, otherwise
     */
    boolean hasKeys();

    /**
     * Replies true if there is a tag with key <code>key</code>.
     * The value could however be empty. See {@link #hasTag(String)} to check for non-empty tags.
     *
     * @param key the key
     * @return true, if there is a tag with key <code>key</code>
     * @see #hasTag(String)
     * @since 11608
     */
    default boolean hasKey(String key) {
        return get(key) != null;
    }

    /**
     * Replies true if there is a non-empty tag with key <code>key</code>.
     *
     * @param key the key
     * @return true, if there is a non-empty tag with key <code>key</code>
     * @see Tagged#hasKey(String)
     * @since 13430
     */
    default boolean hasTag(String key) {
        String v = get(key);
        return v != null && !v.isEmpty();
    }

    /**
     * Replies the set of keys
     *
     * @return the set of keys
     */
    Collection<String> keySet();

    /**
     * Removes all tags
     */
    void removeAll();
}
