// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Column model used in {@link NodeListTable}.
 * @since 1622
 */
public class NodeListColumnModel extends DefaultTableColumnModel {

    /**
     * Constructs a new {@code NodeListColumnModel}.
     * @param renderer table cell renderer
     */
    public NodeListColumnModel(TableCellRenderer renderer) {
        createColumns(renderer);
    }

    protected final void createColumns(TableCellRenderer renderer) {

        // column 0 - Row num
        TableColumn col = new TableColumn(0);
        col.setHeaderValue("");
        col.setResizable(true);
        col.setPreferredWidth(32);
        col.setMaxWidth(32);    // Up to 4 digits (OSM API capabilities -> waynodes maximum set to 2000)
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Node
        col = new TableColumn(1);
        col.setHeaderValue(tr("Node"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
