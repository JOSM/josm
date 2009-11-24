// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class TagMergeColumnModel extends DefaultTableColumnModel {

    protected void createColumns(TableCellRenderer renderer) {

        TableColumn col = null;

        // column 0 - Key
        col = new TableColumn(0);
        col.setHeaderValue(tr("Key"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Value
        col = new TableColumn(1);
        col.setHeaderValue(tr("Value"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public TagMergeColumnModel(TableCellRenderer renderer) {
        createColumns(renderer);
    }
}
