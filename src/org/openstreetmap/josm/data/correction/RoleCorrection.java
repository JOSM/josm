// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.correction;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * Represents a change of a single {@link RelationMember} role.
 * @since 1001
 */
public class RoleCorrection implements Correction {

    /** OSM relation */
    public final Relation relation;
    /** Relation member index */
    public final int position;
    /** Relation member */
    public final RelationMember member;
    /** New role */
    public final String newRole;

    /**
     * Constructs a new {@code RoleCorrection}.
     * @param relation OSM relation
     * @param position relation member index
     * @param member relation member
     * @param newRole new role
     */
    public RoleCorrection(Relation relation, int position, RelationMember member, String newRole) {
        this.relation = relation;
        this.position = position;
        this.member = member;
        this.newRole = newRole;
    }
}
