// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

public class SelectionTableColumnModel  extends DefaultTableColumnModel {
    public SelectionTableColumnModel(MemberTableModel model) {
        TableColumn col = null;
        SelectionTableCellRenderer renderer = new SelectionTableCellRenderer();
        renderer.setMemberTableModel(model);

        // column 0 - the member role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Selection"));
        col.setMinWidth(200);
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
