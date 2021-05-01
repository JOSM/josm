// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.time.Instant;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;

/**
 * Represents an immutable OSM node in the context of a historical view on OSM data.
 * @since 1670
 */
public class HistoryNode extends HistoryOsmPrimitive {

    /** the coordinates. May be null for deleted nodes */
    private LatLon coords;

    /**
     * Constructs a new {@code HistoryNode}.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the node is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required)
     * @param timestamp the timestamp (!= null required)
     * @param coords the coordinates
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryNode(long id, long version, boolean visible, User user, long changesetId, Instant timestamp, LatLon coords) {
        this(id, version, visible, user, changesetId, timestamp, coords, true);
    }

    /**
     * Constructs a new {@code HistoryNode} with a configurable checking of historic parameters.
     * This is needed to build virtual HistoryNodes for modified nodes, which do not have a timestamp and a changeset id.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the node is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @param coords the coordinates
     * @param checkHistoricParams if true, checks values of {@code changesetId} and {@code timestamp}
     * @throws IllegalArgumentException if preconditions are violated
     * @since 5440
     */
    public HistoryNode(long id, long version, boolean visible, User user, long changesetId, Instant timestamp, LatLon coords,
            boolean checkHistoricParams) {
        super(id, version, visible, user, changesetId, timestamp, checkHistoricParams);
        setCoords(coords);
    }

    /**
     * Constructs a new {@code HistoryNode} from an existing {@link Node}.
     * @param n the node
     */
    public HistoryNode(Node n) {
        super(n);
        setCoords(n.getCoor());
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    /**
     * Replies the coordinates. May be null.
     * @return the coordinates. May be null.
     */
    public final LatLon getCoords() {
        return coords;
    }

    /**
     * Sets the coordinates. Can be null.
     * @param coords the coordinates. Can be null.
     */
    public final void setCoords(LatLon coords) {
        this.coords = coords;
    }

    @Override
    public String getDisplayName(HistoryNameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Fills the node attributes with values from this history.
     * @param data node data to fill
     * @return filled node data
     * @since 11878
     */
    public NodeData fillPrimitiveData(NodeData data) {
        super.fillPrimitiveCommonData(data);
        data.setCoor(coords);
        return data;
    }
}
