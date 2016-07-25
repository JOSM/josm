// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The {@link javax.swing.table.TableColumnModel} for the table with the list of tags
 * @since 1709
 */
public class TagTableColumnModel extends DefaultTableColumnModel {
    protected static final int COLUMN_KEY = 0;
    protected static final int COLUMN_VALUE = 1;

    /**
     * Constructs a new {@code TagTableColumnModel}.
     */
    public TagTableColumnModel() {
        createColumns();
    }

    protected void createColumns() {
        TagTableCellRenderer renderer = new TagTableCellRenderer();

        // column 0 - Key
        TableColumn col = new TableColumn(0);
        col.setHeaderValue(tr("Key"));
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Value
        col = new TableColumn(1);
        col.setHeaderValue(tr("Value"));
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
