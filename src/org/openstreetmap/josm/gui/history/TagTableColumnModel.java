// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The {@link javax.swing.table.TableColumnModel} for the table with the list of tags
 *
 */
public class TagTableColumnModel extends DefaultTableColumnModel{
    protected void createColumns() {
        TableColumn col = null;

        TagTableCellRenderer renderer = new TagTableCellRenderer();

        // column 0 - Name
        col = new TableColumn(0);
        col.setHeaderValue(tr("Name"));
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Value
        col = new TableColumn(1);
        col.setHeaderValue(tr("Value"));
        col.setCellRenderer(renderer);
        addColumn(col);

    }

    public TagTableColumnModel() {
        createColumns();
    }
}
