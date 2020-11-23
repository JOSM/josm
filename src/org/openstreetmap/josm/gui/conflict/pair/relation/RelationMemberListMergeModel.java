// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.conflict.RelationMemberConflictResolverCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.ListRole;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * The model for merging two lists of relation members
 * @since 1631
 */
public class RelationMemberListMergeModel extends AbstractListMergeModel<RelationMember, RelationMemberConflictResolverCommand> {

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
            RelationMember memberNew = new RelationMember((String) value, memberOld.getMember());
            getMergedEntries().remove(row);
            getMergedEntries().add(row, memberNew);
            fireModelDataChanged();
        }
    }

    /**
     * populates the model with the relation members in relation my and their
     *
     * @param my my relation. Must not be null.
     * @param their their relation. Must not be null.
     * @param mergedMap The map of merged primitives if the conflict results from merging two layers
     *
     * @throws IllegalArgumentException if my is null
     * @throws IllegalArgumentException if their is null
     */
    public void populate(Relation my, Relation their, Map<PrimitiveId, PrimitiveId> mergedMap) {
        initPopulate(my, their, mergedMap);

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
        return new RelationMember(entry.getRole(), getMyPrimitive(entry));
    }

    @Override
    public OsmPrimitive getMyPrimitive(RelationMember entry) {
        return getMyPrimitiveById(entry.getMember());
    }

    @Override
    public RelationMemberConflictResolverCommand buildResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        CheckParameterUtil.ensureParameterNotNull(conflict, "conflict");
        if (!isFrozen())
            throw new IllegalArgumentException(tr("Merged members not frozen yet. Cannot build resolution command"));
        return new RelationMemberConflictResolverCommand(conflict, new ArrayList<>(getMergedEntries()));
    }
}
