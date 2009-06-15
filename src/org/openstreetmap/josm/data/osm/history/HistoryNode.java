// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.util.Date;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;


/**
 * Represents an immutable OSM node in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryNode extends HistoryOsmPrimitive {
    public HistoryNode(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp) {
        super(id, version, visible, user, uid, changesetId, timestamp);
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }
}
