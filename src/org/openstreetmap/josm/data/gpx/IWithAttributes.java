// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;

/**
 * Object with attributes (in the context of GPX data).
 */
public interface IWithAttributes {

    /**
     * Returns the Object value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value
     */
    Object get(String key);

    /**
     * Returns the String value to which the specified key is mapped,
     * or {@code null} if this map contains no String mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the String value to which the specified key is mapped,
     *         or {@code null} if this map contains no String mapping for the key
     */
    String getString(String key);

    /**
     * Returns the Collection value to which the specified key is mapped,
     * or {@code null} if this map contains no Collection mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the Collection value to which the specified key is mapped,
     *         or {@code null} if this map contains no Collection mapping for the key
     * @since 5502
     */
    Collection<?> getCollection(String key);

    /**
     * Put a key / value pair as a new attribute.
     *
     * Overrides key / value pair with the same key (if present).
     *
     * @param key the key
     * @param value the value
     */
    void put(String key, Object value);

    /**
     * Add a key / value pair that is not part of the GPX schema as an extension.
     *
     * @param key the key
     * @param value the value
     */
    void addExtension(String key, String value);

}
