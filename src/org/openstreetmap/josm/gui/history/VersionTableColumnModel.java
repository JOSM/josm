// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.SwingConstants;
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
        VersionTable.RadioButtonRenderer bRenderer = new VersionTable.RadioButtonRenderer();

        // column 0 - Version
        col = new TableColumn(0);
        /* translation note: 3 letter abbr. for "Version" */
        col.setHeaderValue(tr("Ver"));
        col.setCellRenderer(new VersionTable.AlignedRenderer(SwingConstants.CENTER));
        col.setResizable(false);
        addColumn(col);
        // column 1 - Reverence
        col = new TableColumn(1);
        col.setHeaderValue(tr("A"));
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setResizable(false);
        addColumn(col);
        // column 2 - Current
        col = new TableColumn(2);
        col.setHeaderValue(tr("B"));
        col.setCellRenderer(bRenderer);
        col.setCellEditor(new VersionTable.RadioButtonEditor());
        col.setResizable(false);
        addColumn(col);
        // column 3 - CT state
        col = new TableColumn(3);
        /* translation note: short for "Contributor Terms" */
        col.setHeaderValue(tr("CT"));
        col.setCellRenderer(new VersionTable.LabelRenderer());
        col.setPreferredWidth(22);
        col.setResizable(false);
        addColumn(col);
        // column 4 - Date
        col = new TableColumn(4);
        col.setHeaderValue(tr("Date"));
        col.setResizable(false);
        addColumn(col);
        // column 5 - User
        col = new TableColumn(5);
        col.setHeaderValue(tr("User"));
        col.setResizable(false);
        addColumn(col);
    }

    public VersionTableColumnModel() {
        createColumns();
    }
}
