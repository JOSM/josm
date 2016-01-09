// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Objects;

public class RelationMemberData implements PrimitiveId {

    private final String role;
    private final long memberId;
    private final OsmPrimitiveType memberType;

    public RelationMemberData(String role, OsmPrimitiveType type, long id) {
        this.role = role == null ? "" : role;
        this.memberType = type;
        this.memberId = id;
    }

    public RelationMemberData(String role, PrimitiveId primitive) {
        this(role, primitive.getType(), primitive.getUniqueId());
    }

    public long getMemberId() {
        return memberId;
    }

    public String getRole() {
        return role;
    }

    public OsmPrimitiveType getMemberType() {
        return memberType;
    }

    public boolean hasRole() {
        return !"".equals(role);
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
        return memberType;
    }

    /**
     * PrimitiveId implementation. Returns the same value as {@link #getMemberId()}
     */
    @Override
    public long getUniqueId() {
        return memberId;
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
                Objects.equals(role, that.role) &&
                memberType == that.memberType;
    }
}
