// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.conflict;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PropertyConflict extends ConflictItem {
    private String key;

    public PropertyConflict(String key) {
        this.key = key;
    }

    @Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
        String k = key.get(this.key);
        String v = value.get(this.key);
        return k == null ? v!=null : !k.equals(v);
    }

    @Override protected String str(OsmPrimitive osm) {
        String v = osm.get(key);
        return v == null ? "" : v;
    }

    @Override public String key() {
        return "key|"+key;
    }

    @Override public void apply(OsmPrimitive target, OsmPrimitive other) {
        target.put(key, other.get(key));
        target.version = Math.max(target.version, other.version);
    }
}