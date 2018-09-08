// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The column model for the changeset table
 * @since 2689
 */
public class ChangesetCacheTableColumnModel extends DefaultTableColumnModel {

    private final ChangesetCacheTableCellRenderer renderer = new ChangesetCacheTableCellRenderer();

    protected void createColumn(int modelIndex, String headerValue, int preferredWidth, int width) {
        TableColumn col = new TableColumn(modelIndex);
        col.setHeaderValue(headerValue);
        col.setResizable(true);
        if (width > -1) {
            col.setWidth(width);
        }
        col.setPreferredWidth(preferredWidth);
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    protected void createColumns() {

        // column 0 - Id
        createColumn(0, tr("ID"), 20, 20);

        // column 1 - Upload comment
        createColumn(1, tr("Comment"), 200, -1);

        // column 2 - Open
        createColumn(2, tr("Open"), 25, -1);

        // column 3 - User
        createColumn(3, tr("User"), 50, -1);

        // column 4 - Created at
        createColumn(4, tr("Created at"), 100, -1);

        // column 5 - Closed at
        createColumn(5, tr("Closed at"), 100, -1);

        // column 6 - Changes
        createColumn(6, tr("Changes"), 25, -1);

        // column 7 - Discussions
        createColumn(7, tr("Discussions"), 25, -1);
    }

    /**
     * Creates a new {@code ChangesetCacheTableColumnModel}.
     */
    public ChangesetCacheTableColumnModel() {
        createColumns();
    }
}
