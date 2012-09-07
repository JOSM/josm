// License: GPL.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for various classes in the GPX model.
 *
 * @author Frederik Ramm <frederik@remote.org>
 * @since 444
 */
public class WithAttributes {

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
    public Collection<?> getCollection(String key) {
        Object value = attr.get(key);
        return (value instanceof Collection<?>) ? (Collection<?>)value : null;
    }
}
