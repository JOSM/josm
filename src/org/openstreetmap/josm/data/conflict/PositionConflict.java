// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PositionConflict extends ConflictItem {

    @Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
        return key instanceof Node && !((Node)key).getCoor().equals(((Node)value).getCoor());
    }

    @Override protected String str(OsmPrimitive osm) {
        return osm instanceof Node ? ((Node)osm).getCoor().lat()+", "+((Node)osm).getCoor().lon() : null;
    }

    @Override public String key() {
        return "node|"+tr("position");
    }

    @Override public void apply(OsmPrimitive target, OsmPrimitive other) {
        if (target instanceof Node) {
            ((Node)target).setEastNorth(((Node)other).getEastNorth());
            int newversion = Math.max(target.version, other.version);
            // set version on "other" as well in case user decides to keep local
            target.version = newversion;
            other.version = newversion;
        }
    }
}
