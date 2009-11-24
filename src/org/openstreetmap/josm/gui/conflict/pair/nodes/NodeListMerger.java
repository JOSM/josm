package org.openstreetmap.josm.gui.conflict.pair.nodes;

import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.ListMerger;

/**
 * A UI component for resolving conflicts in the node lists of two {@see Way}s.
 *
 */
public class NodeListMerger extends ListMerger<Node> {
    private static final Logger logger = Logger.getLogger(NodeListMerger.class.getName());

    public NodeListMerger() {
        super(new NodeListMergeModel());
    }

    @Override
    protected JScrollPane buildMyElementsTable() {
        myEntriesTable  = new JTable(
                model.getMyTableModel(),
                new NodeListColumnModel(
                        new NodeListTableCellRenderer()
                ),
                model.getMySelectionModel()
        );
        myEntriesTable.setName("table.mynodes");
        myEntriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        return embeddInScrollPane(myEntriesTable);
    }

    @Override
    protected JScrollPane buildMergedElementsTable() {
        mergedEntriesTable  = new JTable(
                model.getMergedTableModel(),
                new NodeListColumnModel(
                        new NodeListTableCellRenderer()
                ),
                model.getMergedSelectionModel()
        );
        mergedEntriesTable.setName("table.mergednodes");
        mergedEntriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        return embeddInScrollPane(mergedEntriesTable);
    }

    @Override
    protected JScrollPane buildTheirElementsTable() {
        theirEntriesTable  = new JTable(
                model.getTheirTableModel(),
                new NodeListColumnModel(
                        new NodeListTableCellRenderer()
                ),
                model.getTheirSelectionModel()
        );
        theirEntriesTable.setName("table.theirnodes");
        theirEntriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        return embeddInScrollPane(theirEntriesTable);
    }

    public void populate(Way my, Way their) {
        ((NodeListMergeModel)model).populate(my, their);
    }
}
