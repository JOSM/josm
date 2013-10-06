// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;

/**
 * A linkage class that can be used by an relation to keep a list of
 * members. Since membership may be qualified by a "role", a simple
 * list is not sufficient.
 *
 */
public class RelationMember implements PrimitiveId {

    /**
     *
     */
    private final String role;

    /**
     *
     */
    private final OsmPrimitive member;

    /**
     * Returns the role of this relation member.
     * @return Role name or "". Never returns null
     * @since 1930
     */
    public String getRole() {
        return role;
    }

    /**
     * Determines if this relation member has a role.
     * @return True if role is set
     * @since 1930
     */
    public boolean hasRole() {
        return !"".equals(role);
    }

    /**
     * Determines if this relation member's role is in the given list.
     * @param roles The roles to look after
     * @return True if role is in the given list
     * @since 6305
     */
    public boolean hasRole(String ... roles) {
        return Arrays.asList(roles).contains(role);
    }

    /**
     * Determines if this relation member is a relation.
     * @return True if member is relation
     * @since 1937
     */
    public boolean isRelation() {
        return member instanceof Relation;
    }

    /**
     * Determines if this relation member is a way.
     * @return True if member is way
     * @since 1937
     */
    public boolean isWay() {
        return member instanceof Way;
    }

    /**
     *
     * @return type of member for icon display
     * @since 3844
     */
    public OsmPrimitiveType getDisplayType() {
        return member.getDisplayType();
    }

    /**
     * Determines if this relation member is a node.
     * @return True if member is node
     * @since 1937
     */
    public boolean isNode() {
        return member instanceof Node;
    }

    /**
     * Returns the relation member as a relation.
     * @return Member as relation
     * @since 1937
     */
    public Relation getRelation() {
        return (Relation)member;
    }

    /**
     * Returns the relation member as a way.
     * @return Member as way
     * @since 1937
     */
    public Way getWay() {
        return (Way)member;
    }

    /**
     * Returns the relation member as a node.
     * @return Member as node
     * @since 1937
     */
    public Node getNode() {
        return (Node)member;
    }

    /**
     * Returns the relation member.
     * @return Member. Returned value is never null.
     * @since 1937
     */
    public OsmPrimitive getMember() {
        return member;
    }

    /**
     * Constructs a new {@code RelationMember}.
     * @param role Can be null, in this case it's save as ""
     * @param member Cannot be null
     * @throws IllegalArgumentException thrown if member is <code>null</code>
     */
    public RelationMember(String role, OsmPrimitive member) throws IllegalArgumentException{
        if (role == null) {
            role = "";
        }
        if (member == null)
            throw new IllegalArgumentException("Relation member cannot be null");
        this.role = role;
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

    @Override public String toString() {
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
        final int prime = 31;
        int result = 1;
        result = prime * result + member.hashCode();
        result = prime * result + role.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RelationMember) {
            RelationMember other = (RelationMember) obj;
            return member.equals(other.getMember()) && role.equals(other.getRole());
        } else
            return false;
    }

    /**
     * PrimitiveId implementation. Returns the same value as getMember().getType()
     */
    @Override
    public OsmPrimitiveType getType() {
        return member.getType();
    }

    /**
     * PrimitiveId implementation. Returns the same value as getMemberType().getUniqueId()
     */
    @Override
    public long getUniqueId() {
        return member.getUniqueId();
    }

    @Override
    public boolean isNew() {
        return member.isNew();
    }
}
