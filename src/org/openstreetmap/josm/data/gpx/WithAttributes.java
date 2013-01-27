// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation for IWithAttributes.
 *
 * Base class for various classes in the GPX model.
 *
 * @author Frederik Ramm <frederik@remote.org>
 * @since 444
 */
public class WithAttributes implements IWithAttributes, GpxConstants {

    /**
     * The "attr" hash is used to store the XML payload (not only XML attributes!)
     */
    public Map<String, Object> attr = new HashMap<String, Object>(0);

    /**
     * Returns the String value to which the specified key is mapped,
     * or {@code null} if this map contains no String mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the String value to which the specified key is mapped,
     *         or {@code null} if this map contains no String mapping for the key
     */
    @Override
    public String getString(String key) {
        Object value = attr.get(key);
        return (value instanceof String) ? (String)value : null;
    }

    /**
     * Returns the Collection value to which the specified key is mapped,
     * or {@code null} if this map contains no Collection mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the Collection value to which the specified key is mapped,
     *         or {@code null} if this map contains no Collection mapping for the key
     * @since 5502
     */
    @Override
    public Collection<?> getCollection(String key) {
        Object value = attr.get(key);
        return (value instanceof Collection<?>) ? (Collection<?>)value : null;
    }
}
