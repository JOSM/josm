package org.openstreetmap.josm.data.osm;

/**
 * A linkage class that can be used by an relation to keep a list of
 * members. Since membership may be qualified by a "role", a simple
 * list is not sufficient.
 *
 */
public class RelationMember {

    /**
     *
     */
    private final String role;

    /**
     *
     */
    private final OsmPrimitive member;

    /**
     *
     * @return Role name or "". Never returns null
     * @since 1930
     */
    public String getRole() {
        return role;
    }

    /**
     *
     * @return True if role is set
     * @since 1930
     */
    public boolean hasRole() {
        return !"".equals(role);
    }

    /**
     *
     * @return True if member is relation
     * @since 1937
     */
    public boolean isRelation() {
        return member instanceof Relation;
    }

    /**
     *
     * @return True if member is way
     * @since 1937
     */
    public boolean isWay() {
        return member instanceof Way;
    }

    /**
     *
     * @return True if member is node
     * @since 1937
     */
    public boolean isNode() {
        return member instanceof Node;
    }

    /**
     *
     * @return Member as relation
     * @since 1937
     */
    public Relation getRelation() {
        return (Relation)member;
    }

    /**
     *
     * @return Member as way
     * @since 1937
     */
    public Way getWay() {
        return (Way)member;
    }

    /**
     *
     * @return Member as node
     * @since 1937
     */
    public Node getNode() {
        return (Node)member;
    }

    /**
     *
     * @return Member. Returned value is never null.
     * @since 1937
     */
    public OsmPrimitive getMember() {
        return member;
    }

    /**
     *
     * @param role Can be null, in this case it's save as ""
     * @param member Cannot be null
     * @throw IllegalArgumentException thrown if member is null
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
}
