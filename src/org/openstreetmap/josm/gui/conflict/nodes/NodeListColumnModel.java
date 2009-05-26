// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.nodes;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class NodeListColumnModel extends DefaultTableColumnModel {

    protected void createColumns(TableCellRenderer renderer) {
        
        TableColumn col = null;
        
        // column 0 - Node  
        col = new TableColumn(0);
        col.setHeaderValue(tr("Node"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);        
    }

    public NodeListColumnModel(TableCellRenderer renderer) {
        createColumns(renderer);
    }
}
