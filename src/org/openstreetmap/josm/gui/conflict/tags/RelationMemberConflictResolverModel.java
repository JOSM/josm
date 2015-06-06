// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * This model manages a list of conflicting relation members.
 *
 * It can be used as {@link javax.swing.table.TableModel}.
 */
public class RelationMemberConflictResolverModel extends DefaultTableModel {
    /** the property name for the number conflicts managed by this model */
    public static final String NUM_CONFLICTS_PROP = RelationMemberConflictResolverModel.class.getName() + ".numConflicts";

    /** the list of conflict decisions */
    protected final transient List<RelationMemberConflictDecision> decisions;
    /** the collection of relations for which we manage conflicts */
    protected transient Collection<Relation> relations;
    /** the number of conflicts */
    private int numConflicts;
    private final PropertyChangeSupport support;

    /**
     * Replies true if each {@link MultiValueResolutionDecision} is decided.
     *
     * @return true if each {@link MultiValueResolutionDecision} is decided; false otherwise
     */
    public boolean isResolvedCompletely() {
        return numConflicts == 0;
    }

    /**
     * Replies the current number of conflicts
     *
     * @return the current number of conflicts
     */
    public int getNumConflicts() {
        return numConflicts;
    }

    /**
     * Updates the current number of conflicts from list of decisions and emits
     * a property change event if necessary.
     *
     */
    protected void updateNumConflicts() {
        int count = 0;
        for (RelationMemberConflictDecision decision: decisions) {
            if (!decision.isDecided()) {
                count++;
            }
        }
        int oldValue = numConflicts;
        numConflicts = count;
        if (numConflicts != oldValue) {
            support.firePropertyChange(getProperty(), oldValue, numConflicts);
        }
    }

    protected String getProperty() {
        return NUM_CONFLICTS_PROP;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    public RelationMemberConflictResolverModel() {
        decisions = new ArrayList<>();
        support = new PropertyChangeSupport(this);
    }

    @Override
    public int getRowCount() {
        return getNumDecisions();
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (decisions == null) return null;

        RelationMemberConflictDecision d = decisions.get(row);
        switch(column) {
        case 0: /* relation */ return d.getRelation();
        case 1: /* pos */ return Integer.toString(d.getPos() + 1); // position in "user space" starting at 1
        case 2: /* role */ return d.getRole();
        case 3: /* original */ return d.getOriginalPrimitive();
        case 4: /* decision */ return d.getDecision();
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        RelationMemberConflictDecision d = decisions.get(row);
        switch(column) {
        case 2: /* role */
            d.setRole((String)value);
            break;
        case 4: /* decision */
            d.decide((RelationMemberConflictDecisionType)value);
            refresh();
            break;
        }
        fireTableDataChanged();
    }

    /**
     * Populates the model with the members of the relation <code>relation</code>
     * referring to <code>primitive</code>.
     *
     * @param relation the parent relation
     * @param primitive the child primitive
     */
    protected void populate(Relation relation, OsmPrimitive primitive) {
        for (int i =0; i<relation.getMembersCount();i++) {
            if (relation.getMember(i).refersTo(primitive)) {
                decisions.add(new RelationMemberConflictDecision(relation, i));
            }
        }
    }

    /**
     * Populates the model with the relation members belonging to one of the relations in <code>relations</code>
     * and referring to one of the primitives in <code>memberPrimitives</code>.
     *
     * @param relations  the parent relations. Empty list assumed if null.
     * @param memberPrimitives the child primitives. Empty list assumed if null.
     */
    public void populate(Collection<Relation> relations, Collection<? extends OsmPrimitive> memberPrimitives) {
        decisions.clear();
        relations = relations == null ? new LinkedList<Relation>() : relations;
        memberPrimitives = memberPrimitives == null ? new LinkedList<OsmPrimitive>() : memberPrimitives;
        for (Relation r : relations) {
            for (OsmPrimitive p: memberPrimitives) {
                populate(r,p);
            }
        }
        this.relations = relations;
        refresh();
    }

    /**
     * Populates the model with the relation members represented as a collection of
     * {@link RelationToChildReference}s.
     *
     * @param references the references. Empty list assumed if null.
     */
    public void populate(Collection<RelationToChildReference> references) {
        references = references == null ? new LinkedList<RelationToChildReference>() : references;
        decisions.clear();
        this.relations = new HashSet<>(references.size());
        for (RelationToChildReference reference: references) {
            decisions.add(new RelationMemberConflictDecision(reference.getParent(), reference.getPosition()));
            relations.add(reference.getParent());
        }
        refresh();
    }

    /**
     * Replies the decision at position <code>row</code>
     *
     * @param row position
     * @return the decision at position <code>row</code>
     */
    public RelationMemberConflictDecision getDecision(int row) {
        return decisions.get(row);
    }

    /**
     * Replies the number of decisions managed by this model
     *
     * @return the number of decisions managed by this model
     */
    public int getNumDecisions() {
        return decisions == null ? 0 : decisions.size();
    }

    /**
     * Refreshes the model state. Invoke this method to trigger necessary change
     * events after an update of the model data.
     *
     */
    public void refresh() {
        updateNumConflicts();
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                fireTableDataChanged();
            }
        });
    }

    /**
     * Apply a role to all member managed by this model.
     *
     * @param role the role. Empty string assumed if null.
     */
    public void applyRole(String role) {
        role = role == null ? "" : role;
        for (RelationMemberConflictDecision decision : decisions) {
            decision.setRole(role);
        }
        refresh();
    }

    protected RelationMemberConflictDecision getDecision(Relation relation, int pos) {
        for(RelationMemberConflictDecision decision: decisions) {
            if (decision.matches(relation, pos)) return decision;
        }
        return null;
    }

    protected Command buildResolveCommand(Relation relation, OsmPrimitive newPrimitive) {
        final Relation modifiedRelation = new Relation(relation);
        modifiedRelation.setMembers(null);
        boolean isChanged = false;
        for (int i=0; i < relation.getMembersCount(); i++) {
            final RelationMember member = relation.getMember(i);
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                modifiedRelation.addMember(member);
            } else {
                switch(decision.getDecision()) {
                case KEEP:
                    final RelationMember newMember = new RelationMember(decision.getRole(),newPrimitive);
                    modifiedRelation.addMember(newMember);
                    isChanged |= !member.equals(newMember);
                    break;
                case REMOVE:
                    isChanged = true;
                    // do nothing
                    break;
                case UNDECIDED:
                    // FIXME: this is an error
                    break;
                }
            }
        }
        if (isChanged)
            return new ChangeCommand(relation, modifiedRelation);
        return null;
    }

    /**
     * Builds a collection of commands executing the decisions made in this model.
     *
     * @param newPrimitive the primitive which members shall refer to
     * @return a list of commands
     */
    public List<Command> buildResolutionCommands(OsmPrimitive newPrimitive) {
        List<Command> command = new LinkedList<>();
        for (Relation relation : relations) {
            Command cmd = buildResolveCommand(relation, newPrimitive);
            if (cmd != null) {
                command.add(cmd);
            }
        }
        return command;
    }

    protected boolean isChanged(Relation relation, OsmPrimitive newPrimitive) {
        for (int i=0; i < relation.getMembersCount(); i++) {
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                continue;
            }
            switch(decision.getDecision()) {
            case REMOVE: return true;
            case KEEP:
                if (!relation.getMember(i).getRole().equals(decision.getRole()))
                    return true;
                if (relation.getMember(i).getMember() != newPrimitive)
                    return true;
            case UNDECIDED:
                // FIXME: handle error
            }
        }
        return false;
    }

    /**
     * Replies the set of relations which have to be modified according
     * to the decisions managed by this model.
     *
     * @param newPrimitive the primitive which members shall refer to
     *
     * @return the set of relations which have to be modified according
     * to the decisions managed by this model
     */
    public Set<Relation> getModifiedRelations(OsmPrimitive newPrimitive) {
        Set<Relation> ret = new HashSet<>();
        for (Relation relation: relations) {
            if (isChanged(relation, newPrimitive)) {
                ret.add(relation);
            }
        }
        return ret;
    }
}
