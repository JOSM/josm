// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents an immutable OSM relation in the context of a historical view on OSM data.
 * @since 1670
 */
public class HistoryRelation extends HistoryOsmPrimitive {

    private final List<RelationMemberData> members = new ArrayList<>();

    /**
     * constructor
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required)
     * @param timestamp the timestamp (!= null required)
     *
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryRelation(long id, long version, boolean visible, User user, long changesetId, Instant timestamp) {
        super(id, version, visible, user, changesetId, timestamp);
    }

    /**
     * constructor
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required if {@code checkHistoricParams} is true)
     * @param timestamp the timestamp (!= null required if {@code checkHistoricParams} is true)
     * @param checkHistoricParams If true, checks values of {@code changesetId} and {@code timestamp}
     *
     * @throws IllegalArgumentException if preconditions are violated
     * @since 5440
     */
    public HistoryRelation(long id, long version, boolean visible, User user, long changesetId, Instant timestamp, boolean checkHistoricParams) {
        super(id, version, visible, user, changesetId, timestamp, checkHistoricParams);
    }

    /**
     * constructor
     *
     * @param id the id (&gt; 0 required)
     * @param version the version (&gt; 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (!= null required)
     * @param changesetId the changeset id (&gt; 0 required)
     * @param timestamp the timestamp (!= null required)
     * @param members list of members for this relation
     *
     * @throws IllegalArgumentException if preconditions are violated
     */
    public HistoryRelation(long id, long version, boolean visible, User user, long changesetId, Instant timestamp,
            List<RelationMemberData> members) {
        this(id, version, visible, user, changesetId, timestamp);
        if (members != null) {
            this.members.addAll(members);
        }
    }

    /**
     * Constructs a new {@code HistoryRelation} from an existing {@link Relation}.
     * @param r the relation
     */
    public HistoryRelation(Relation r) {
        super(r);
    }

    /**
     * replies an immutable list of members of this relation
     *
     * @return an immutable list of members of this relation
     */
    public List<RelationMemberData> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * replies the number of members
     *
     * @return the number of members
     *
     */
    public int getNumMembers() {
        return members.size();
    }

    /**
     * replies the idx-th member
     * @param idx the index
     * @return the idx-th member
     * @throws IndexOutOfBoundsException if idx is out of bounds
     */
    public RelationMemberData getRelationMember(int idx) {
        if (idx < 0 || idx >= members.size())
            throw new IndexOutOfBoundsException(
                    MessageFormat.format("Parameter {0} not in range 0..{1}. Got ''{2}''.", "idx", members.size(), idx));
        return members.get(idx);
    }

    /**
     * replies the type, i.e. {@link OsmPrimitiveType#RELATION}
     *
     */
    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }

    /**
     * adds a member to the list of members
     *
     * @param member the member (must not be null)
     * @throws IllegalArgumentException if member is null
     */
    public void addMember(RelationMemberData member) {
        CheckParameterUtil.ensureParameterNotNull(member, "member");
        members.add(member);
    }

    @Override
    public String getDisplayName(HistoryNameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Fills the relation attributes with values from this history.
     * @param data relation data to fill
     * @return filled relation data
     * @since 11878
     */
    public RelationData fillPrimitiveData(RelationData data) {
        super.fillPrimitiveCommonData(data);
        data.setMembers(members);
        return data;
    }
}
