// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public interface PrimitiveId {

    long getUniqueId();
    OsmPrimitiveType getType();

    /**
     * Replies true if this id represents a new primitive.
     * 
     * @return true if this id represents a new primitive.
     */
    boolean isNew();

}
