// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

public class RelationMemberData {

    private String role;
    private long memberId;
    private OsmPrimitiveType memberType;

    public RelationMemberData() {

    }

    public RelationMemberData(String role, OsmPrimitive primitive) {
        this.role = role;
        this.memberId = primitive.getUniqueId();
        this.memberType = OsmPrimitiveType.from(primitive);
    }

    public long getMemberId() {
        return memberId;
    }
    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public OsmPrimitiveType getMemberType() {
        return memberType;
    }
    public void setMemberType(OsmPrimitiveType memberType) {
        this.memberType = memberType;
    }

    @Override
    public String toString() {
        return memberType.getAPIName() + " " + memberId;
    }

}
