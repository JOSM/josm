// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents an immutable OSM way in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryWay extends HistoryOsmPrimitive {

    private List<Long> nodeIds = new ArrayList<Long>();

    /**
     * Constructs a new {@code HistoryWay}.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the node is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryWay(long id, long version, boolean visible, User user, long changesetId, Date timestamp) throws IllegalArgumentException {
        super(id, version, visible, user, changesetId, timestamp);
    }

    /**
     * Constructs a new {@code HistoryWay} with a configurable checking of historic parameters.
     * This is needed to build virtual HistoryWays for modified ways, which do not have a timestamp and a changeset id.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the node is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @param checkHistoricParams if true, checks values of {@code changesetId} and {@code timestamp}
     * @throws IllegalArgumentException if preconditions are violated
     * @since 5440
     */
    public HistoryWay(long id, long version, boolean visible, User user, long changesetId, Date timestamp, boolean checkHistoricParams) throws IllegalArgumentException {
        super(id, version, visible, user, changesetId, timestamp, checkHistoricParams);
    }

    /**
     * Constructs a new {@code HistoryWay} with a given list of node ids.
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the node is still visible
     * @param user the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @param nodeIdList the node ids (!= null required)
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryWay(long id, long version, boolean visible, User user, long changesetId, Date timestamp, List<Long> nodeIdList) throws IllegalArgumentException {
        this(id, version, visible, user, changesetId, timestamp);
        CheckParameterUtil.ensureParameterNotNull(nodeIdList, "nodeIdList");
        this.nodeIds.addAll(nodeIdList);
    }

    /**
     * Constructs a new {@code HistoryWay} from an existing {@link Way}.
     * @param w the way
     */
    public HistoryWay(Way w) {
        super(w);
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
     * @exception IndexOutOfBoundsException thrown, if  idx &lt; 0 || idx &gt;= {#see {@link #getNumNodes()}
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
     * replies the ways type, i.e. {@link OsmPrimitiveType#WAY}
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

    /**
     * Replies true if this way is closed.
     *
     * @return true if this way is closed.
     */
    public boolean isClosed() {
        return getNumNodes() >= 3 && nodeIds.get(0) == nodeIds.get(nodeIds.size()-1);
    }

    @Override
    public String getDisplayName(HistoryNameFormatter formatter) {
        return formatter.format(this);
    }
}
