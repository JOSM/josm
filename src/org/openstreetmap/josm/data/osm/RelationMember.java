package org.openstreetmap.josm.data.osm;

/**
 * A linkage class that can be used by an relation to keep a list of
 * members. Since membership may be qualified by a "role", a simple
 * list is not sufficient.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class RelationMember {

    public String role;
    public OsmPrimitive member;

    /**
     *
     * @return Role name or "". Never returns null
     * @since 1930
     */
    public String getRole() {
        if (role == null) {
            return "";
        } else {
            return role;
        }
    }

    /**
     *
     * @return True if role is set
     * @since 1930
     */
    public boolean hasRole() {
        return role != null && !"".equals(role);
    }

    /**
     * Default constructor. Does nothing.
     * @deprecated Use other constructors because RelationMember class will became immutable
     * in the future
     */
    @Deprecated
    public RelationMember() { }

    public RelationMember(String role, OsmPrimitive member) {
        this.role = role;
        this.member = member;
    }

    /**
     * Copy constructor.
     * @param other relation member to be copied.
     */
    public RelationMember(RelationMember other) {
        role = other.role;
        member = other.member;
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
        if (primitive == null) return false;
        if (member == null) return false;
        return member == primitive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((member == null) ? 0 : member.hashCode());
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
        RelationMember other = (RelationMember) obj;
        if (member == null) {
            if (other.member != null)
                return false;
        } else if (!member.equals(other.member))
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        return true;
    }
}
