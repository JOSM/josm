// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

public class RoleCorrection implements Correction {

    public final Relation relation;
    public final int position;
    public final RelationMember member;
    public final String newRole;

    public RoleCorrection(Relation relation, int position,
                          RelationMember member, String newRole) {
        this.relation = relation;
        this.position = position;
        this.member = member;
        this.newRole = newRole;
    }
}
