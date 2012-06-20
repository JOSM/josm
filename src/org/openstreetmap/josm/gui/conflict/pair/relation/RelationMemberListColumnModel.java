// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class RelationMemberListColumnModel extends DefaultTableColumnModel{

    protected void createColumns() {
        TableColumn col = null;
        RelationMemberTableCellRenderer renderer = new RelationMemberTableCellRenderer();

        // column 0 - Row num
        col = new TableColumn(0);
        col.setHeaderValue("");
        col.setResizable(true);
        col.setPreferredWidth(32);
        col.setMaxWidth(40);    // Up to 5 digits
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - Role
        col = new TableColumn(1);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setMaxWidth(100);
        col.setCellEditor(new RelationMemberTableCellEditor());
        addColumn(col);

        // column 2 - Primitive
        col = new TableColumn(2);
        col.setHeaderValue(tr("Primitive"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public RelationMemberListColumnModel() {
        createColumns();
    }
}
