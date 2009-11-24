// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
/**
 * Represents an immutable OSM way in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryWay extends HistoryOsmPrimitive {

    private ArrayList<Long> nodeIds;

    public HistoryWay(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp) {
        super(id, version, visible, user, uid, changesetId, timestamp);
        nodeIds = new ArrayList<Long>();
    }

    public HistoryWay(long id, long version, boolean visible, String user, long uid, long changesetId, Date timestamp, ArrayList<Long> nodeIdList) {
        this(id, version, visible, user, uid, changesetId, timestamp);
        this.nodeIds.addAll(nodeIdList);
    }

    /**
     * replies the number of nodes in this way
     * @return the number of nodes
     */
    public int getNumNodes() {
        return nodeIds.size();
    }

    /**
     * replies the idx-th node id in the list of node ids of this way
     *
     * @param idx the index
     * @return the idx-th node id
     * @exception IndexOutOfBoundsException thrown, if  idx <0 || idx >= {#see {@link #getNumNodes()}
     */
    public long getNodeId(int idx) throws IndexOutOfBoundsException {
        if (idx < 0 || idx >= nodeIds.size())
            throw new IndexOutOfBoundsException(tr("Parameter {0} not in range 0..{1}. Got ''{2}''.", "idx", nodeIds.size(),idx));
        return nodeIds.get(idx);
    }

    /**
     * replies an immutable list of the ways node ids
     *
     * @return the ways node ids
     */
    public List<Long> getNodes() {
        return Collections.unmodifiableList(nodeIds);
    }

    /**
     * replies the ways type, i.e. {@see OsmPrimitiveType#WAY}
     *
     * @return the ways type
     */
    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }

    /**
     * adds a node id to the list nodes of this way
     *
     * @param ref the node id to add
     */
    public void addNode(long ref) {
        nodeIds.add(ref);
    }
}
