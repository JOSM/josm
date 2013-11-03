// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecisionType.UNDECIDED;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class RelationMemberConflictDecision {

    private Relation relation;
    private int pos;
    private OsmPrimitive originalPrimitive;
    private String role;
    private RelationMemberConflictDecisionType decision;

    public RelationMemberConflictDecision(Relation relation, int pos) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(relation, "relation");
        RelationMember member = relation.getMember(pos);
        if (member == null)
            throw new IndexOutOfBoundsException(tr("Position {0} is out of range. Current number of members is {1}.", pos, relation.getMembersCount()));
        this.relation = relation;
        this.pos  = pos;
        this.originalPrimitive = member.getMember();
        this.role = member.hasRole()? member.getRole() : "";
        this.decision = UNDECIDED;
    }

    public Relation getRelation() {
        return relation;
    }

    public int getPos() {
        return pos;
    }

    public OsmPrimitive getOriginalPrimitive() {
        return originalPrimitive;
    }

    public String getRole() {
        return role;
    }

    public RelationMemberConflictDecisionType getDecision() {
        return decision;
    }

    public void setRole(String role) {
        this.role = role == null ? "" : role;
    }

    public void decide(RelationMemberConflictDecisionType decision) {
        if (decision == null) {
            decision = UNDECIDED;
        }
        this.decision = decision;
    }

    public boolean isDecided() {
        return ! UNDECIDED.equals(decision);
    }

    public boolean matches(Relation relation, int pos) {
        return this.relation == relation && this.pos == pos;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((decision == null) ? 0 : decision.hashCode());
        result = prime * result + ((originalPrimitive == null) ? 0 : originalPrimitive.hashCode());
        result = prime * result + pos;
        result = prime * result + ((relation == null) ? 0 : relation.hashCode());
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
        RelationMemberConflictDecision other = (RelationMemberConflictDecision) obj;
        if (decision == null) {
            if (other.decision != null)
                return false;
        } else if (!decision.equals(other.decision))
            return false;
        if (originalPrimitive == null) {
            if (other.originalPrimitive != null)
                return false;
        } else if (!originalPrimitive.equals(other.originalPrimitive))
            return false;
        if (pos != other.pos)
            return false;
        if (relation == null) {
            if (other.relation != null)
                return false;
        } else if (!relation.equals(other.relation))
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        return true;
    }
}
