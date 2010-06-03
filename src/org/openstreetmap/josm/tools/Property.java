// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Small interface to define a property with both read and write access.
 */
public interface Property<ObjectType, PropertyType> {
    /**
     * Get the value of the property.
     * @param obj the object, from that the property is derived
     * @return the value of the property for the object obj
     */
    public PropertyType get(ObjectType obj);
    /**
     * Set the value of the property for the object.
     * @param obj the object for that the property should be set
     * @param value the value the property is set to
     */
    public void set(ObjectType obj, PropertyType value);
}
