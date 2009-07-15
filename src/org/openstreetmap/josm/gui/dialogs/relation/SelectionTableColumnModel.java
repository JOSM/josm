// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.OsmPrimitivRenderer;

public class SelectionTableColumnModel  extends DefaultTableColumnModel {
    public SelectionTableColumnModel() {
        TableColumn col = null;
        OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

        // column 0 - the member role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Selection"));
        col.setResizable(true);
        col.setCellRenderer(renderer);
        addColumn(col);
    }
}
