// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The column model for the changeset content
 * @since 7715
 */
public class ChangesetDiscussionTableColumnModel extends DefaultTableColumnModel {

    /**
     * Constructs a new {@code ChangesetContentTableColumnModel}.
     */
    public ChangesetDiscussionTableColumnModel() {
        createColumns();
    }

    protected void createColumns() {
        ChangesetDiscussionTableCellRenderer renderer = new ChangesetDiscussionTableCellRenderer();
        // column 0 - Date
        TableColumn col = new TableColumn(0, 150);
        col.setHeaderValue(tr("Date"));
        col.setResizable(true);
        col.setMaxWidth(200);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - User
        col = new TableColumn(1, 150);
        col.setHeaderValue(tr("User"));
        col.setResizable(true);
        col.setMaxWidth(300);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 2 - Text
        col = new TableColumn(2, 400);
        col.setHeaderValue(tr("Comment"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
