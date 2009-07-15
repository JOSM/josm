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
     * Default constructor. Does nothing.
     */
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
