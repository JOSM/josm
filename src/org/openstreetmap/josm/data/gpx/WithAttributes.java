// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation for IWithAttributes.
 *
 * Base class for various classes in the GPX model.
 *
 * @author Frederik Ramm
 * @since 444
 */
public class WithAttributes implements IWithAttributes, GpxConstants {

    /**
     * The "attr" hash is used to store the XML payload (not only XML attributes!)
     */
    public Map<String, Object> attr = new HashMap<>(0);

    /**
     * The "exts" collection contains all extensions.
     */
    private final GpxExtensionCollection exts = new GpxExtensionCollection(this);

    /**
     * Returns the Object value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value
     */
    @Override
    public Object get(String key) {
        return attr.get(key);
    }

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
        return (value instanceof String) ? (String) value : null;
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
    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> getCollection(String key) {
        Object value = attr.get(key);
        return (value instanceof Collection) ? (Collection<T>) value : null;
    }

    /**
     * Put a key / value pair as a new attribute.
     * Overrides key / value pair with the same key (if present).
     *
     * @param key the key
     * @param value the value
     */
    @Override
    public void put(String key, Object value) {
        attr.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attr;
    }

    @Override
    public GpxExtensionCollection getExtensions() {
        return exts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attr, exts);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WithAttributes other = (WithAttributes) obj;
        if (attr == null) {
            if (other.attr != null)
                return false;
        } else if (!attr.equals(other.attr))
            return false;
        if (exts == null) {
            if (other.exts != null)
                return false;
        } else if (!exts.equals(other.exts))
            return false;
        return true;
    }
}
