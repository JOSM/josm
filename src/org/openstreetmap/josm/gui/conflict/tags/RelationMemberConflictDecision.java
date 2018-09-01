// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecisionType.UNDECIDED;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This class stores the decision the user made regarding a relation member conflict
 */
public class RelationMemberConflictDecision {

    private final Relation relation;
    private final int pos;
    private final OsmPrimitive originalPrimitive;
    private String role;
    private RelationMemberConflictDecisionType decision;

    public RelationMemberConflictDecision(Relation relation, int pos) {
        CheckParameterUtil.ensureParameterNotNull(relation, "relation");
        RelationMember member = relation.getMember(pos);
        if (member == null)
            throw new IndexOutOfBoundsException(
                    tr("Position {0} is out of range. Current number of members is {1}.", pos, relation.getMembersCount()));
        this.relation = relation;
        this.pos = pos;
        this.originalPrimitive = member.getMember();
        this.role = member.hasRole() ? member.getRole() : "";
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
        this.decision = Optional.ofNullable(decision).orElse(UNDECIDED);
    }

    public boolean isDecided() {
        return UNDECIDED != decision;
    }

    public boolean matches(Relation relation, int pos) {
        return this.relation == relation && this.pos == pos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relation, pos, originalPrimitive, role, decision);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RelationMemberConflictDecision that = (RelationMemberConflictDecision) obj;
        return pos == that.pos &&
               decision == that.decision &&
               Objects.equals(relation, that.relation) &&
               Objects.equals(originalPrimitive, that.originalPrimitive) &&
               Objects.equals(role, that.role);
    }

    @Override
    public String toString() {
        return originalPrimitive.getPrimitiveId() + " at index " + pos + " with role " + role + " in " + relation.getUniqueId()
            + " => " + decision;
    }
}
