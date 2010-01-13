// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.RelationMemberConflictResolverCommand;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.ListRole;
import org.openstreetmap.josm.tools.CheckParameterUtil;
/**
 * The model for merging two lists of relation members
 *
 */
public class RelationMemberListMergeModel extends ListMergeModel<RelationMember>{

    private static final Logger logger = Logger.getLogger(RelationMemberListMergeModel.class.getName());

    @Override
    public boolean isEqualEntry(RelationMember e1, RelationMember e2) {
        return e1.equals(e2);
    }

    @Override
    protected void buildMergedEntriesTableModel() {
        // the table model for merged entries is different because it supports
        // editing cells in the first column
        //
        mergedEntriesTableModel = this.new EntriesTableModel(ListRole.MERGED_ENTRIES) {
            @Override
            public boolean isCellEditable(int row, int column) {
                switch(column) {
                case 1: return true;
                default: return false;
                }
            }
        };
    }

    @Override
    protected void setValueAt(DefaultTableModel model, Object value, int row, int col) {
        if (model == getMergedTableModel() && col == 1) {
            RelationMember memberOld = getMergedEntries().get(row);
            RelationMember memberNew = new RelationMember((String)value,memberOld.getMember());
            getMergedEntries().remove(row);
            getMergedEntries().add(row,memberNew);
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
        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");

        getMergedEntries().clear();
        getMyEntries().clear();
        getTheirEntries().clear();

        for (RelationMember n : my.getMembers()) {
            getMyEntries().add(n);
        }
        for (RelationMember n : their.getMembers()) {
            getTheirEntries().add(n);
        }
        if (myAndTheirEntriesEqual()) {
            for (RelationMember m : getMyEntries()) {
                getMergedEntries().add(cloneEntryForMergedList(m));
            }
            setFrozen(true);
        } else {
            setFrozen(false);
        }

        fireModelDataChanged();
    }

    @Override
    protected RelationMember cloneEntryForMergedList(RelationMember entry) {
        return new RelationMember(entry);
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
        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");
        if (! isFrozen())
            throw new IllegalArgumentException(tr("Merged nodes not frozen yet. Cannot build resolution command"));
        ArrayList<RelationMember> entries = getMergedEntries();
        return new RelationMemberConflictResolverCommand(my, their, entries);
    }
}
