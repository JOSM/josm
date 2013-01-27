// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Collection;

/**
 * Object with attributes.
 */
public interface IWithAttributes {

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
}
