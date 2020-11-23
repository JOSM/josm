// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.command.conflict.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMerger;

/**
 * A UI component for resolving conflicts in the node lists of two {@link Way}s.
 * @since 1622
 */
public class NodeListMerger extends AbstractListMerger<Node, WayNodesConflictResolverCommand> {

    /**
     * Constructs a new {@code NodeListMerger}.
     */
    public NodeListMerger() {
        super(new NodeListMergeModel());
    }

    @Override
    protected JScrollPane buildMyElementsTable() {
        myEntriesTable = new NodeListTable(
                "table.mynodes",
                model,
                model.getMyTableModel(),
                model.getMySelectionModel()
        );
        return embedInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        mergedEntriesTable = new NodeListTable(
                "table.mergednodes",
                model,
                model.getMergedTableModel(),
                model.getMergedSelectionModel()
        );
        return embedInScrollPane(mergedEntriesTable);
    }

    @Override
    protected JScrollPane buildTheirElementsTable() {
        theirEntriesTable = new NodeListTable(
                "table.theirnodes",
                model,
                model.getTheirTableModel(),
                model.getTheirSelectionModel()
        );
        return embedInScrollPane(theirEntriesTable);
    }

    @Override
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        Way myWay = (Way) conflict.getMy();
        Way theirWay = (Way) conflict.getTheir();
        ((NodeListMergeModel) model).populate(myWay, theirWay, conflict.getMergedMap());
        myEntriesTable.setLayer(findLayerFor(myWay));
        theirEntriesTable.setLayer(findLayerFor(theirWay));
    }

    @Override
    public void deletePrimitive(boolean deleted) {
        if (deleted) {
            model.setFrozen(true);
            model.clearMerged();
        } else {
            model.setFrozen(false);
        }
    }
}
