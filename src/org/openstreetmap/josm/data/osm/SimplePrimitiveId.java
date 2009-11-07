// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class SimplePrimitiveId implements PrimitiveId {

    private final long id;
    private final OsmPrimitiveType type;

    public SimplePrimitiveId(long id, OsmPrimitiveType type) {
        this.id = id;
        this.type = type;
    }

    public OsmPrimitiveType getType() {
        return type;
    }

    public long getUniqueId() {
        return id;
    }



}
