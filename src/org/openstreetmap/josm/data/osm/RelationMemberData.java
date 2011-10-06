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
     * PrimitiveId implementation. Returns the same value as {@link #getMemberId()()}
     */
    @Override
    public long getUniqueId() {
        return memberId;
    }

    @Override
    public boolean isNew() {
        return memberId <= 0;
    }
}
