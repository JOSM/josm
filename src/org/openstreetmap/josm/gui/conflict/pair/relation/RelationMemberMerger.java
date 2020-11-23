// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.command.conflict.RelationMemberConflictResolverCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMerger;

/**
 * A UI component for resolving conflicts in the member lists of two {@link Relation}s.
 * @since 1631
 */
public class RelationMemberMerger extends AbstractListMerger<RelationMember, RelationMemberConflictResolverCommand> {

    /**
     * Constructs a new {@code RelationMemberMerger}.
     */
    public RelationMemberMerger() {
        super(new RelationMemberListMergeModel());
    }

    @Override
    protected JScrollPane buildMyElementsTable() {
        myEntriesTable = new RelationMemberTable(
                "table.mymembers",
                model,
                model.getMyTableModel(),
                model.getMySelectionModel()
        );
        return embedInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        mergedEntriesTable = new RelationMemberTable(
                "table.mergedmembers",
                model,
                model.getMergedTableModel(),
                model.getMergedSelectionModel()
        );
        mergedEntriesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        return embedInScrollPane(mergedEntriesTable);
    }

    @Override
    protected JScrollPane buildTheirElementsTable() {
        theirEntriesTable = new RelationMemberTable(
                "table.theirmembers",
                model,
                model.getTheirTableModel(),
                model.getTheirSelectionModel()
        );
        return embedInScrollPane(theirEntriesTable);
    }

    @Override
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        Relation myRel = (Relation) conflict.getMy();
        Relation theirRel = (Relation) conflict.getTheir();
        ((RelationMemberListMergeModel) model).populate(myRel, theirRel, conflict.getMergedMap());
        myEntriesTable.setLayer(findLayerFor(myRel));
        theirEntriesTable.setLayer(findLayerFor(theirRel));
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
