// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class NodeListColumnModel extends DefaultTableColumnModel {

    protected void createColumns(TableCellRenderer renderer) {

        TableColumn col = null;

        // column 0 - Row num
        col = new TableColumn(0);
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

    public NodeListColumnModel(TableCellRenderer renderer) {
        createColumns(renderer);
    }
}
