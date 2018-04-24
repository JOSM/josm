// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.Objects;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the data (role, type and id) that is stored in the database for a given relation member.
 * @since 2284
 */
public class RelationMemberData implements IRelationMember, Serializable {

    private static final long serialVersionUID = 381392198209333319L;
    private final String role;
    private final long memberId;
    private final OsmPrimitiveType memberType;

    /**
     * Constructs a new {@code RelationMemberData}.
     * @param role member role - can be null
     * @param type member type - cannot be null
     * @param id member id - cannot be null
     * @throws IllegalArgumentException is type or id is null
     */
    public RelationMemberData(String role, OsmPrimitiveType type, long id) {
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        this.role = role == null ? "" : role;
        this.memberType = type;
        this.memberId = id;
    }

    /**
     * Constructs a new {@code RelationMemberData}.
     * @param role member role - can be null
     * @param primitive member type and id - cannot be null
     * @throws NullPointerException if primitive is null
     */
    public RelationMemberData(String role, PrimitiveId primitive) {
        this(role, primitive.getType(), primitive.getUniqueId());
    }

    /**
     * Get member id.
     * @return member id
     */
    public long getMemberId() {
        return memberId;
    }

    @Override
    public String getRole() {
        return role;
    }

    /**
     * Get member type.
     * @return member type
     */
    public OsmPrimitiveType getMemberType() {
        return memberType;
    }

    @Override
    public boolean isNode() {
        return OsmPrimitiveType.NODE == memberType;
    }

    @Override
    public boolean isWay() {
        return OsmPrimitiveType.WAY == memberType;
    }

    @Override
    public boolean isRelation() {
        return OsmPrimitiveType.RELATION == memberType;
    }

    @Override
    public String toString() {
        return (memberType != null ? memberType.getAPIName() : "undefined") + ' ' + memberId;
    }

    /**
     * PrimitiveId implementation. Returns the same value as {@link #getMemberType()}
     */
    @Override
    public OsmPrimitiveType getType() {
        return getMemberType();
    }

    /**
     * PrimitiveId implementation. Returns the same value as {@link #getMemberId()}
     */
    @Override
    public long getUniqueId() {
        return getMemberId();
    }

    @Override
    public boolean isNew() {
        return memberId <= 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, memberId, memberType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationMemberData that = (RelationMemberData) obj;
        return memberId == that.memberId &&
               memberType == that.memberType &&
               Objects.equals(role, that.role);
    }
}
