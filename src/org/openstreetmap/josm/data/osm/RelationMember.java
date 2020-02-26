// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A linkage class that can be used by an relation to keep a list of members.
 * Since membership may be qualified by a "role", a simple list is not sufficient.
 * @since 343
 */
public class RelationMember implements IRelationMember<OsmPrimitive> {

    /**
     *
     */
    private final String role;

    /**
     *
     */
    private final OsmPrimitive member;

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public boolean isRelation() {
        return member instanceof Relation;
    }

    @Override
    public boolean isWay() {
        return member instanceof Way;
    }

    @Override
    public boolean isNode() {
        return member instanceof Node;
    }

    /**
     * Returns the relation member as a relation.
     * @return Member as relation
     * @since 1937
     */
    public Relation getRelation() {
        return (Relation) member;
    }

    /**
     * Returns the relation member as a way.
     * @return Member as way
     * @since 1937
     */
    public Way getWay() {
        return (Way) member;
    }

    /**
     * Returns the relation member as a node.
     * @return Member as node
     * @since 1937
     */
    public Node getNode() {
        return (Node) member;
    }

    @Override
    public OsmPrimitive getMember() {
        return member;
    }

    /**
     * Constructs a new {@code RelationMember}.
     * @param role Can be null, in this case it's save as ""
     * @param member Cannot be null
     * @throws IllegalArgumentException if member is <code>null</code>
     */
    public RelationMember(String role, OsmPrimitive member) {
        CheckParameterUtil.ensureParameterNotNull(member, "member");
        this.role = Optional.ofNullable(role).orElse("").intern();
        this.member = member;
    }

    /**
     * Copy constructor.
     * This constructor is left only for backwards compatibility. Copying RelationMember doesn't make sense
     * because it's immutable
     * @param other relation member to be copied.
     */
    public RelationMember(RelationMember other) {
        this(other.role, other.member);
    }

    @Override
    public String toString() {
        return '"' + role + "\"=" + member;
    }

    /**
     * Replies true, if this relation member refers to the primitive
     *
     * @param primitive  the primitive to check
     * @return true, if this relation member refers to the primitive
     */
    public boolean refersTo(OsmPrimitive primitive) {
        return member == primitive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, member);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationMember that = (RelationMember) obj;
        return Objects.equals(role, that.role) &&
               Objects.equals(member, that.member);
    }

    /**
     * PrimitiveId implementation. Returns the same value as getMember().getType()
     */
    @Override
    public OsmPrimitiveType getType() {
        return member.getType();
    }

    /**
     * PrimitiveId implementation. Returns the same value as getMember().getUniqueId()
     */
    @Override
    public long getUniqueId() {
        return member.getUniqueId();
    }

    /**
     * PrimitiveId implementation. Returns the same value as getMember().isNew()
     */
    @Override
    public boolean isNew() {
        return member.isNew();
    }
}
