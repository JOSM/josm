// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class RelationMemberListColumnModel extends DefaultTableColumnModel{

    protected void createColumns() {
        TableColumn col = null;
        RelationMemberTableCellRenderer renderer = new RelationMemberTableCellRenderer();

        // column 0 - Role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        col.setCellEditor(new RelationMemberTableCellEditor());
        addColumn(col);

        // column 1 - Primitive
        col = new TableColumn(1);
        col.setHeaderValue(tr("Primitive"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }

    public RelationMemberListColumnModel() {
        createColumns();
    }
}
