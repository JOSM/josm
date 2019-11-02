// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.Map;

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
     * @param <T> type of items
     *
     * @param key the key whose associated value is to be returned
     * @return the Collection value to which the specified key is mapped,
     *         or {@code null} if this map contains no Collection mapping for the key
     * @since 5502
     */
    <T> Collection<T> getCollection(String key);

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
     * Returns the attributes
     * @return the attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Returns the extensions
     * @return the extensions
     */
    GpxExtensionCollection getExtensions();

}
