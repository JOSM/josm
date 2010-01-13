// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents an immutable OSM relation in the context of a historical view on
 * OSM data.
 *
 */
public class HistoryRelation extends HistoryOsmPrimitive{

    private ArrayList<RelationMember> members;

    /**
     * constructor
     *
     * @param id the id (>0 required)
     * @param version the version (> 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (! null required)
     * @param uid the user id (> 0 required)
     * @param changesetId the changeset id (> 0 required)
     * @param timestamp the timestamp (! null required)
     *
     * @throws IllegalArgumentException thrown if preconditions are violated
     */
    public HistoryRelation(long id, long version, boolean visible, String user, long uid, long changesetId,
            Date timestamp) throws IllegalArgumentException {
        super(id, version, visible, user, uid, changesetId, timestamp);
        members = new ArrayList<RelationMember>();
    }
    /**
     * constructor
     *
     * @param id the id (>0 required)
     * @param version the version (> 0 required)
     * @param visible whether the primitive is still visible
     * @param user  the user (! null required)
     * @param uid the user id (> 0 required)
     * @param changesetId the changeset id (> 0 required)
     * @param timestamp the timestamp (! null required)
     * @param members list of members for this relation
     *
     * @throws IllegalArgumentException thrown if preconditions are violated
     */
    public HistoryRelation(long id, long version, boolean visible, String user, long uid, long changesetId,
            Date timestamp, ArrayList<RelationMember> members) {
        this(id, version, visible, user, uid, changesetId, timestamp);
        if (members != null) {
            this.members.addAll(members);
        }
    }

    /**
     * replies an immutable list of members of this relation
     *
     * @return an immutable list of members of this relation
     */
    public List<RelationMember> getMembers() {
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
     * @throws IndexOutOfBoundsException thrown, if idx is out of bounds
     */
    public RelationMember getRelationMember(int idx) throws IndexOutOfBoundsException  {
        if (idx < 0 || idx >= members.size())
            throw new IndexOutOfBoundsException(MessageFormat.format("Parameter {0} not in range 0..{1}. Got ''{2}''.", "idx", members.size(),idx));
        return members.get(idx);
    }

    /**
     * replies the type, i.e. {@see OsmPrimitiveType#RELATION}
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
     * @exception IllegalArgumentException thrown, if member is null
     */
    public void addMember(RelationMember member) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(member, "member");
        members.add(member);
    }

    @Override
    public String getDisplayName(HistoryNameFormatter formatter) {
        return formatter.format(this);
    }
}
