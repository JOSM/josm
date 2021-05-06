// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.Optional;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Relation members for a Vector Relation
 */
public class VectorRelationMember implements IRelationMember<VectorPrimitive> {
    private final String role;
    private final VectorPrimitive member;

    /**
     * Create a new relation member
     * @param role The role of the member
     * @param member The member primitive
     */
    public VectorRelationMember(String role, VectorPrimitive member) {
        CheckParameterUtil.ensureParameterNotNull(member, "member");
        this.role = Optional.ofNullable(role).orElse("").intern();
        this.member = member;
    }

    @Override
    public String getRole() {
        return this.role;
    }

    @Override
    public boolean isNode() {
        return this.member instanceof INode;
    }

    @Override
    public boolean isWay() {
        return this.member instanceof IWay;
    }

    @Override
    public boolean isRelation() {
        return this.member instanceof IRelation;
    }

    @Override
    public VectorPrimitive getMember() {
        return this.member;
    }

    @Override
    public long getUniqueId() {
        return this.member.getId();
    }

    @Override
    public OsmPrimitiveType getType() {
        return this.member.getType();
    }

    @Override
    public boolean isNew() {
        return this.member.isNew();
    }
}
