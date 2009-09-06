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

public class RelationMemberConflictResolverModel extends DefaultTableModel {
    static public final String NUM_CONFLICTS_PROP = RelationMemberConflictResolverModel.class.getName() + ".numConflicts";

    private List<RelationMemberConflictDecision> decisions;
    private Collection<Relation> relations;
    private int numConflicts;
    private PropertyChangeSupport support;


    public int getNumConflicts() {
        return numConflicts;
    }

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
            support.firePropertyChange(NUM_CONFLICTS_PROP, oldValue, numConflicts);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    public RelationMemberConflictResolverModel() {
        decisions = new ArrayList<RelationMemberConflictDecision>();
        support = new PropertyChangeSupport(this);
    }

    @Override
    public int getRowCount() {
        if (decisions == null) return 0;
        return decisions.size();
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

    protected void populate(Relation relation, OsmPrimitive primitive) {
        for (int i =0; i<relation.getMembersCount();i++) {
            if (relation.getMember(i).refersTo(primitive)) {
                decisions.add(new RelationMemberConflictDecision(relation, i));
            }
        }
    }

    public void populate(Collection<Relation> relations, Collection<? extends OsmPrimitive> memberPrimitives) {
        decisions.clear();
        for (Relation r : relations) {
            for (OsmPrimitive p: memberPrimitives) {
                populate(r,p);
            }
        }
        this.relations = relations;
        refresh();
    }

    public RelationMemberConflictDecision getDecision(int row) {
        return decisions.get(row);
    }

    public int getNumDecisions() {
        return  getRowCount();
    }

    public void refresh() {
        updateNumConflicts();
        fireTableDataChanged();
    }

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
        Relation modifiedRelation = new Relation(relation);
        modifiedRelation.setMembers(null);
        boolean isChanged = false;
        for (int i=0; i < relation.getMembersCount(); i++) {
            RelationMember rm = relation.getMember(i);
            RelationMember rmNew;
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                modifiedRelation.addMember(rm);
            } else {
                switch(decision.getDecision()) {
                    case REPLACE:
                        rmNew = new RelationMember(decision.getRole(),newPrimitive);
                        modifiedRelation.addMember(rmNew);
                        isChanged |= ! rm.equals(rmNew);
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

    public List<Command> buildResolutionCommands(OsmPrimitive newPrimitive) {
        List<Command> command = new LinkedList<Command>();
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
                case REPLACE:
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

    public Set<Relation> getModifiedRelations(OsmPrimitive newPrimitive) {
        HashSet<Relation> ret = new HashSet<Relation>();
        for (Relation relation: relations) {
            if (isChanged(relation, newPrimitive)) {
                ret.add(relation);
            }
        }
        return ret;
    }
}
