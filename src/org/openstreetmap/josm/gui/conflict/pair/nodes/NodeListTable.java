// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.command.conflict.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.AbstractListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.PairTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

/**
 * Node list table.
 * @since 5297
 */
public class NodeListTable extends PairTable {

    /**
     * Constructs a new {@code NodeListTable}.
     * @param name table name
     * @param model node merge model
     * @param dm table model
     * @param sm selection model
     */
    public NodeListTable(String name, AbstractListMergeModel<Node, WayNodesConflictResolverCommand> model,
            OsmPrimitivesTableModel dm, ListSelectionModel sm) {
        super(name, model, dm, new NodeListColumnModel(new NodeListTableCellRenderer()), sm);
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }
}
