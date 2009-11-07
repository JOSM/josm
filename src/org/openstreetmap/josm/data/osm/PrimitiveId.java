// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public interface PrimitiveId {

    long getUniqueId();

    OsmPrimitiveType getType();

}
