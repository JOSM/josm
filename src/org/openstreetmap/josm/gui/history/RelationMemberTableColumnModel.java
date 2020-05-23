// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * The {@link javax.swing.table.TableColumnModel} for the table with the list of relation members.
 * @since 1709
 */
public class RelationMemberTableColumnModel extends DefaultTableColumnModel {

    static final int INDEX_COLUMN = 0;
    static final int ROLE_COLUMN = 1;
    static final int OBJECT_COLUMN = 2;

    /**
     * Constructs a new {@code RelationMemberTableColumnModel}.
     */
    public RelationMemberTableColumnModel() {
        createColumns();
    }

    protected void createColumns() {
        RelationMemberListTableCellRenderer renderer = new RelationMemberListTableCellRenderer();

        TableColumn col = new TableColumn(INDEX_COLUMN, 0);
        col.setHeaderValue(tr("\u2116"));
        col.setCellRenderer(renderer);
        addColumn(col);

        col = new TableColumn(ROLE_COLUMN);
        col.setHeaderValue(tr("Role"));
        col.setCellRenderer(renderer);
        addColumn(col);

        col = new TableColumn(OBJECT_COLUMN);
        col.setHeaderValue(tr("Object"));
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
