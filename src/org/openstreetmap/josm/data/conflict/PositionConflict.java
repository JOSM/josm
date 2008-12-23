// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PositionConflict extends ConflictItem {

    @Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
        return key instanceof Node && !((Node)key).coor.equals(((Node)value).coor);
    }

    @Override protected String str(OsmPrimitive osm) {
        return osm instanceof Node ? ((Node)osm).coor.lat()+", "+((Node)osm).coor.lon() : null;
    }

    @Override public String key() {
        return "node|"+tr("position");
    }

    @Override public void apply(OsmPrimitive target, OsmPrimitive other) {
        if (target instanceof Node) {
            ((Node)target).coor = ((Node)other).coor;
            ((Node)target).eastNorth = ((Node)other).eastNorth;
        }
    }
}