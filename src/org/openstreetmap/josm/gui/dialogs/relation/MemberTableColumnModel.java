// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.data.osm.DataSet;

public class MemberTableColumnModel extends DefaultTableColumnModel {

    public MemberTableColumnModel(DataSet ds) {
        TableColumn col = null;

        // column 0 - the member role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setPreferredWidth(100);
        col.setCellRenderer(new MemberTableRoleCellRenderer());
        col.setCellEditor(new MemberRoleCellEditor(ds));
        addColumn(col);

        // column 1 - the member
        col = new TableColumn(1);
        col.setHeaderValue(tr("Refers to"));
        col.setResizable(true);
        col.setPreferredWidth(300);
        col.setCellRenderer(new MemberTableMemberCellRenderer());
        addColumn(col);

        // column 2 -
        col = new TableColumn(2);
        col.setHeaderValue("");
        col.setResizable(false);
        col.setPreferredWidth(20);
        col.setCellRenderer(new MemberTableLinkedCellRenderer());
        addColumn(col);
    }
}
