// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
* interface to represent identification and type of the datatypes primitives.
*
* @since 2399
*/
public interface PrimitiveId {

    /**
     * Gets a unique id representing this object (the OSM server id for OSM objects)
     *
     * @return the id number
     */
    long getUniqueId();

    /**
     * Gets the type of object represented by this object.
     *
     * @return the object type
     * @see Node
     * @see Way
     * @see Relation
     */
    OsmPrimitiveType getType();

    /**
     * Replies true if this id represents a new primitive.
     *
     * @return true if this id represents a new primitive.
     */
    boolean isNew();

}
