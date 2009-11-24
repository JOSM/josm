// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Map;
/**
 * Objects implement Tagged if they provide a map of key/value pairs.
 *
 *
 */
// FIXME: better naming? setTags(), getTags(), getKeys() instead of keySet() ?
//
public interface Tagged {
    /**
     * Sets the map of key/value pairs
     *
     * @param keys the map of key value pairs. If null, reset to the empty map.
     */
    void setKeys(Map<String,String> keys);

    /**
     * Replies the map of key/value pairs. Never null, but may be the empty map.
     *
     * @return the map of key/value pairs
     */
    Map<String,String> getKeys();

    /**
     * Sets a key/value pairs
     *
     * @param key the key
     * @param value the value. If null, removes the key/value pair.
     */
    void put(String key, String value);

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
