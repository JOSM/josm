// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The column model for the changeset table
 *
 */
public class ChangesetCacheTableColumnModel extends DefaultTableColumnModel {

    protected void createColumns() {
        TableColumn col = null;
        ChangesetCacheTableCellRenderer renderer = new ChangesetCacheTableCellRenderer();

        // column 0 - Id
        col = new TableColumn(0);
        col.setHeaderValue("ID");
        col.setResizable(true);
        col.setWidth(20);
        col.setPreferredWidth(20);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Upload comment
        col = new TableColumn(1);
        col.setHeaderValue(tr("Comment"));
        col.setResizable(true);
        col.setPreferredWidth(200);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 2 - Open
        col = new TableColumn(2);
        col.setHeaderValue(tr("Open"));
        col.setResizable(true);
        col.setPreferredWidth(50);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 3 - User
        col = new TableColumn(3);
        col.setHeaderValue(tr("User"));
        col.setResizable(true);
        col.setPreferredWidth(50);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 4 - Created at
        col = new TableColumn(4);
        col.setHeaderValue(tr("Created at"));
        col.setResizable(true);
        col.setPreferredWidth(100);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 5 - Closed at
        col = new TableColumn(4);
        col.setHeaderValue(tr("Closed at"));
        col.setResizable(true);
        col.setPreferredWidth(100);
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public ChangesetCacheTableColumnModel() {
        createColumns();
    }
}
