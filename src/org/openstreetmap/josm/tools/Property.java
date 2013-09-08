// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Small interface to define a property with both read and write access.
 * @param <O> Object type
 * @param <P> Property type
 */
public interface Property<O, P> {
    
    /**
     * Get the value of the property.
     * @param obj the object, from that the property is derived
     * @return the value of the property for the object obj
     */
    public P get(O obj);
    
    /**
     * Set the value of the property for the object.
     * @param obj the object for that the property should be set
     * @param value the value the property is set to
     */
    public void set(O obj, P value);
}
