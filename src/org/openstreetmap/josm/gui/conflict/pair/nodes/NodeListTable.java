// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.PairTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

public class NodeListTable extends PairTable {

    public NodeListTable(String name, ListMergeModel<Node> model, OsmPrimitivesTableModel dm, ListSelectionModel sm) {
        super(name, model, dm, new NodeListColumnModel(new NodeListTableCellRenderer()), sm);
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }
}
