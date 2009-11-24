// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.util.Date;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Represents an immutable OSM node in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryNode extends HistoryOsmPrimitive {
    /** the coordinates */

    private LatLon coords;

    public HistoryNode(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp, LatLon coords) {
        super(id, version, visible, user, uid, changesetId, timestamp);
        setCoords(coords);
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    public LatLon getCoords() {
        return coords;
    }

    public void setCoords(LatLon coords) {
        this.coords = coords;
    }
}
