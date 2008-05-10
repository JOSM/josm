// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class DeleteConflict extends ConflictItem {

	@Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
		return key.deleted != value.deleted;
	}

	@Override public String key() {
		return "deleted|"+tr("deleted");
	}

	@Override protected String str(OsmPrimitive osm) {
		return osm.deleted ? tr("true") : tr("false");
	}

	@Override public void apply(OsmPrimitive target, OsmPrimitive other) {
		target.deleted = other.deleted;
	}
}
