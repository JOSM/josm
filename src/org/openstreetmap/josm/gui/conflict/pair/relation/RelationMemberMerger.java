// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.ListMerger;

/**
 * A UI component for resolving conflicts in the member lists of two {@link Relation}
 */
public class RelationMemberMerger extends ListMerger<RelationMember> implements IConflictResolver {
    @Override
    protected JScrollPane buildMyElementsTable() {
        myEntriesTable  = new RelationMemberTable(
                "table.mymembers",
                model.getMyTableModel(),
                model.getMySelectionModel()
        );
        return embeddInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        mergedEntriesTable  = new RelationMemberTable(
                "table.mergedmembers",
                model.getMergedTableModel(),
                model.getMergedSelectionModel()
        );
        mergedEntriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        return embeddInScrollPane(mergedEntriesTable);
    }

    @Override
    protected JScrollPane buildTheirElementsTable() {
        theirEntriesTable  = new RelationMemberTable(
                "table.theirmembers",
                model.getTheirTableModel(),
                model.getTheirSelectionModel()
        );
        return embeddInScrollPane(theirEntriesTable);
    }

    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        RelationMemberListMergeModel model = (RelationMemberListMergeModel)getModel();
        model.populate((Relation)conflict.getMy(), (Relation)conflict.getTheir(), conflict.getMergedMap());
    }

    public RelationMemberMerger() {
        super(new RelationMemberListMergeModel());
    }

    public void deletePrimitive(boolean deleted) {
        if (deleted) {
            model.clearMerged();
            model.setFrozen(true);
        } else {
            model.setFrozen(false);
        }
    }
}
