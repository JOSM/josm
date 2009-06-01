// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.RelationMemberConflictResolverCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.ListMergeModel;
/**
 * The model for merging two lists of relation members
 * 
 *
 */
public class RelationMemberListMergeModel extends ListMergeModel<RelationMember>{

    private static final Logger logger = Logger.getLogger(RelationMemberListMergeModel.class.getName());

    @Override
    public boolean isEqualEntry(RelationMember e1, RelationMember e2) {
        return
        (    (e1.role == null && e2.role == null)
                || (e1.role != null && e1.role.equals(e2.role))
        )
        && e1.member.id == e2.member.id;
    }

    @Override
    protected void buildMergedEntriesTableModel() {
        // the table model for merged entries is different because it supports
        // editing cells in the first column
        //
        mergedEntriesTableModel = this.new ListTableModel<RelationMember>(mergedEntries) {
            @Override
            public boolean isCellEditable(int row, int column) {
                switch(column) {
                case 0: return true;
                default: return false;
                }
            }
        };
    }

    @Override
    protected void setValueAt(DefaultTableModel model, Object value, int row, int col) {
        if (model == getMergedTableModel() && col == 0) {
            RelationMember member = mergedEntries.get(row);
            member.role = (String)value;
            fireModelDataChanged();
        }
    }

    /**
     * populates the model with the relation members in relation my and their
     * 
     * @param my my relation. Must not be null.
     * @param their their relation. Must not be null.
     * 
     * @throws IllegalArgumentException if my is null
     * @throws IllegalArgumentException if their is null
     */
    public void populate(Relation my, Relation their) {
        if (my == null)
            throw new IllegalArgumentException(tr("parameter way must not be null"));
        if (their == null)
            throw new IllegalArgumentException(tr("parameter their must not be null"));

        mergedEntries.clear();
        myEntries.clear();
        theirEntries.clear();

        for (RelationMember n : my.members) {
            myEntries.add(n);
        }
        for (RelationMember n : their.members) {
            theirEntries.add(n);
        }
        if (myAndTheirEntriesEqual()) {
            for (RelationMember m : myEntries) {
                mergedEntries.add(cloneEntry(m));
            }
            setFrozen(true);
        } else {
            setFrozen(false);
        }

        fireModelDataChanged();
    }

    @Override
    protected RelationMember cloneEntry(RelationMember entry) {
        RelationMember member = new RelationMember();
        member.role = entry.role;
        if (entry.member instanceof Node) {
            member.member = new Node(entry.member.id);
        } else if (entry.member instanceof Way) {
            member.member = new Way(entry.member.id);
        } else if (entry.member instanceof Relation) {
            member.member = new Relation(entry.member.id);
        }
        member.member.cloneFrom(entry.member);
        return member;
    }

    /**
     * Builds the command to resolve conflicts in the node list of a way
     * 
     * @param my  my relation. Must not be null.
     * @param their  their relation. Must not be null
     * @return the command
     * @exception IllegalArgumentException thrown, if my is null
     * @exception IllegalArgumentException thrown, if their is null
     * @exception IllegalStateException thrown, if the merge is not yet frozen
     */
    public RelationMemberConflictResolverCommand buildResolveCommand(Relation my, Relation their) {
        if (my == null)
            throw new IllegalArgumentException(tr("parameter my most not be null"));
        if (their == null)
            throw new IllegalArgumentException(tr("parameter my most not be null"));
        if (! isFrozen())
            throw new IllegalArgumentException(tr("merged nodes not frozen yet. Can't build resolution command"));
        return new RelationMemberConflictResolverCommand(my, their, mergedEntries);
    }
}
