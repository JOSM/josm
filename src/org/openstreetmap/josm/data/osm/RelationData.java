// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

/**
 * Relation data.
 * @since 2284
 */
public class RelationData extends PrimitiveData implements IRelation {

    private static final long serialVersionUID = 1163664954890478565L;
    private List<RelationMemberData> members = new ArrayList<>();

    /**
     * Constructs a new {@code RelationData}.
     */
    public RelationData() {
        // contents can be set later with setters
    }

    /**
     * Constructs a new {@code RelationData} with given id.
     * @param id id
     * @since 12017
     */
    public RelationData(long id) {
        super(id);
    }

    /**
     * Constructs a new {@code RelationData}.
     * @param data relation data to copy
     */
    public RelationData(RelationData data) {
        super(data);
        members.addAll(data.members);
    }

    /**
     * Returns relation members.
     * @return relation members
     */
    public List<RelationMemberData> getMembers() {
        return members;
    }

    /**
     * Sets relation members.
     * @param memberData relation members
     */
    public void setMembers(List<RelationMemberData> memberData) {
        members = new ArrayList<>(memberData);
    }

    @Override
    public int getMembersCount() {
        return members.size();
    }

    @Override
    public long getMemberId(int idx) {
        return members.get(idx).getMemberId();
    }

    @Override
    public String getRole(int idx) {
        return members.get(idx).getRole();
    }

    @Override
    public OsmPrimitiveType getMemberType(int idx) {
        return members.get(idx).getMemberType();
    }

    @Override
    public RelationData makeCopy() {
        return new RelationData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " REL " + members;
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BBox getBBox() {
        throw new UnsupportedOperationException();
    }
}
