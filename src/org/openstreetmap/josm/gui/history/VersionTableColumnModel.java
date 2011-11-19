// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
/**
 * The {@see TableColumnModel} for the table with the list of versions
 *
 */
public class VersionTableColumnModel extends DefaultTableColumnModel {
    protected void createColumns() {
        TableColumn col = null;
        TableCellRenderer renderer = new VersionTableCellRenderer();
        VersionTable.RadioButtonRenderer bRenderer = new VersionTable.RadioButtonRenderer();

        // column 0 - Reverence
        col = new TableColumn(0);
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setMaxWidth(1);
        col.setResizable(false);
        addColumn(col);
        // column 1 - Current
        col = new TableColumn(1);
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setMaxWidth(1);
        col.setResizable(false);
        addColumn(col);
        // column 2 - Rest
        col = new TableColumn(2);
        col.setHeaderValue(tr("Version"));
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public VersionTableColumnModel() {
        createColumns();
    }
}
