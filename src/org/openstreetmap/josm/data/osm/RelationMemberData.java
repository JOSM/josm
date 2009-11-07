// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class RelationMemberData implements PrimitiveId {

    private final String role;
    private final long memberId;
    private final OsmPrimitiveType memberType;

    public RelationMemberData(String role, OsmPrimitiveType type, long id) {
        this.role = role;
        this.memberType = type;
        this.memberId = id;
    }

    public RelationMemberData(String role, OsmPrimitive primitive) {
        this(role, OsmPrimitiveType.from(primitive), primitive.getUniqueId());
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

    @Override
    public String toString() {
        return memberType.getAPIName() + " " + memberId;
    }

    /**
     * PrimitiveId implementation. Returns the same value as {@link #getMemberType()}
     */
    public OsmPrimitiveType getType() {
        return memberType;
    }

    /**
     * PrimitiveId implementation. Returns the same value as {@link #getMemberId()()}
     */
    public long getUniqueId() {
        return memberId;
    }

}
