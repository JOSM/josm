// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The {@link javax.swing.table.TableColumnModel} for the table with the list of nodes.
 * @since 1709
 */
public class NodeListTableColumnModel extends DefaultTableColumnModel {

    /**
     * Constructs a new {@code NodeListTableColumnModel}.
     */
    public NodeListTableColumnModel() {
        createColumns();
    }

    protected void createColumns() {
        NodeListTableCellRenderer renderer = new NodeListTableCellRenderer();

        // column 0 - Nodes
        TableColumn col = new TableColumn(0);
        col.setHeaderValue(tr("Nodes"));
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
