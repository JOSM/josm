// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The {@link javax.swing.table.TableColumnModel} for the table with the list of relation members.
 *
 */
public class RelationMemberTableColumnModel extends DefaultTableColumnModel {
    protected void createColumns() {
        TableColumn col = null;
        RelationMemberListTableCellRenderer renderer = new RelationMemberListTableCellRenderer();

        // column 0 - Version
        col = new TableColumn(0);
        col.setHeaderValue(tr("Role"));
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 0 - Version
        col = new TableColumn(1);
        col.setHeaderValue(tr("Object"));
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public RelationMemberTableColumnModel() {
        createColumns();
    }
}
