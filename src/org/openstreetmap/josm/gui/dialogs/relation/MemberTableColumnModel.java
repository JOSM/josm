// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.OsmPrimitivRenderer;

public class MemberTableColumnModel extends DefaultTableColumnModel{

    public MemberTableColumnModel() {
        TableColumn col = null;
        MemberTableCellRenderer renderer = new MemberTableCellRenderer();

        // column 0 - the member role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setCellRenderer(renderer);

        addColumn(col);

        // column 1 - the member
        col = new TableColumn(1);
        col.setHeaderValue(tr("Refers to"));
        col.setResizable(true);
        col.setCellRenderer(new OsmPrimitivRenderer());
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 2 -
        col = new TableColumn(2);
        col.setHeaderValue(tr("Linked"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
