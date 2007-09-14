// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;

public class ToConflict extends ConflictItem {
	
	@Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
		return key instanceof Segment && !((Segment)key).to.equals(((Segment)value).to);
	}

	@Override protected String str(OsmPrimitive osm) {
		return osm instanceof Segment ? String.valueOf(((Segment)osm).to.id) : null;
	}

	@Override public String key() {
		return "segment|"+tr("to");
	}
	
	@Override public void apply(OsmPrimitive target, OsmPrimitive other) {
		if (target instanceof Segment)
			((Segment)target).to = ((Segment)other).to;
    }
}