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

    public boolean isNew() {
        return id <= 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplePrimitiveId other = (SimplePrimitiveId) obj;
        if (id != other.id)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return type + " " + id;
    }
}
