// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class RelationMemberData implements PrimitiveId {

    private final String role;
    private final long memberId;
    private final OsmPrimitiveType memberType;

    public RelationMemberData(String role, OsmPrimitiveType type, long id) {
        this.role = role == null?"":role;
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
        return (memberType != null ? memberType.getAPIName() : "undefined") + " " + memberId;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (memberId ^ (memberId >>> 32));
        result = prime * result
                + ((memberType == null) ? 0 : memberType.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RelationMemberData other = (RelationMemberData) obj;
        if (memberId != other.memberId)
            return false;
        if (memberType != other.memberType)
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        return true;
    }
}
