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
                model,
                model.getMyTableModel(),
                model.getMySelectionModel()
        );
        return embeddInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        mergedEntriesTable  = new RelationMemberTable(
                "table.mergedmembers",
                model,
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
                model,
                model.getTheirTableModel(),
                model.getTheirSelectionModel()
        );
        return embeddInScrollPane(theirEntriesTable);
    }

    @Override
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        Relation myRel = (Relation)conflict.getMy();
        Relation theirRel = (Relation)conflict.getTheir();
        ((RelationMemberListMergeModel)model).populate(myRel, theirRel, conflict.getMergedMap());
        myEntriesTable.setLayer(findLayerFor(myRel));
        theirEntriesTable.setLayer(findLayerFor(theirRel));
    }

    public RelationMemberMerger() {
        super(new RelationMemberListMergeModel());
    }

    @Override
    public void deletePrimitive(boolean deleted) {
        if (deleted) {
            model.clearMerged();
            model.setFrozen(true);
        } else {
            model.setFrozen(false);
        }
    }
}
