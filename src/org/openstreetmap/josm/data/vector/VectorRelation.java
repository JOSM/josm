// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.UniqueIdGenerator;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

/**
 * The "Relation" type for vectors
 *
 * @author Taylor Smock
 * @since 17862
 */
public class VectorRelation extends VectorPrimitive implements IRelation<VectorRelationMember> {
    private static final UniqueIdGenerator RELATION_ID_GENERATOR = new UniqueIdGenerator();
    private final List<VectorRelationMember> members = new ArrayList<>();
    private BBox cachedBBox;

    /**
     * Create a new relation for a layer
     * @param layer The layer the relation will belong to
     */
    public VectorRelation(String layer) {
        super(layer);
    }

    @Override
    public UniqueIdGenerator getIdGenerator() {
        return RELATION_ID_GENERATOR;
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BBox getBBox() {
        if (this.cachedBBox == null) {
            BBox tBBox = new BBox();
            for (IPrimitive member : this.getMemberPrimitivesList()) {
                tBBox.add(member.getBBox());
            }
            this.cachedBBox = tBBox.toImmutable();
        }
        return this.cachedBBox;
    }

    protected void addRelationMember(VectorRelationMember member) {
        this.members.add(member);
        member.getMember().addReferrer(this);
        cachedBBox = null;
    }

    /**
     * Remove the first instance of a member from the relation
     *
     * @param member The member to remove
     */
    protected void removeRelationMember(VectorRelationMember member) {
        this.members.remove(member);
        if (!this.members.contains(member)) {
            member.getMember().removeReferrer(this);
        }
    }

    @Override
    public int getMembersCount() {
        return this.members.size();
    }

    @Override
    public VectorRelationMember getMember(int index) {
        return this.members.get(index);
    }

    @Override
    public List<VectorRelationMember> getMembers() {
        return Collections.unmodifiableList(this.members);
    }

    @Override
    public void setMembers(List<VectorRelationMember> members) {
        this.members.clear();
        this.members.addAll(members);
        for (VectorRelationMember member : members) {
            member.getMember().addReferrer(this);
        }
        cachedBBox = null;
    }

    @Override
    public long getMemberId(int idx) {
        return this.getMember(idx).getMember().getId();
    }

    @Override
    public String getRole(int idx) {
        return this.getMember(idx).getRole();
    }

    @Override
    public OsmPrimitiveType getMemberType(int idx) {
        return this.getMember(idx).getType();
    }

    @Override
    public OsmPrimitiveType getType() {
        return this.getMembers().stream().map(VectorRelationMember::getType)
          .allMatch(OsmPrimitiveType.CLOSEDWAY::equals) ? OsmPrimitiveType.MULTIPOLYGON : OsmPrimitiveType.RELATION;
    }
}
