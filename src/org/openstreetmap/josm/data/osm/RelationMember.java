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

    @Override public boolean equals(Object other) {
        if (!(other instanceof RelationMember)) return false;
        RelationMember otherMember = (RelationMember) other;
        return otherMember.role.equals(role) && otherMember.member.equals(member);
    }

    @Override public String toString() {
        return '"' + role + "\"=" + member;
    }
}
