// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.util.Date;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.coor.LatLon;


/**
 * Represents an immutable OSM node in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryNode extends HistoryOsmPrimitive {
    private LatLon coor;
    public HistoryNode(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp,
    double lat, double lon) {
        super(id, version, visible, user, uid, changesetId, timestamp);
        coor = new LatLon(lat, lon);
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    public LatLon getCoordinate() {
        return coor;
    }
}
