// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.OsmPrimitivRenderer;

/**
 * The column model for the changeset content
 *
 */
public class ChangesetContentTableColumnModel extends DefaultTableColumnModel {

    protected void createColumns() {
        TableColumn col = null;
        ChangesetContentTableCellRenderer renderer = new ChangesetContentTableCellRenderer();
        // column 0 - type
        col = new TableColumn(0);
        col.setHeaderValue("");
        col.setResizable(true);
        col.setWidth(50);
        col.setPreferredWidth(50);
        col.setMaxWidth(100);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 1 - ID
        col = new TableColumn(1);
        col.setHeaderValue(tr("ID"));
        col.setResizable(true);
        col.setPreferredWidth(60);
        col.setMaxWidth(100);
        col.setCellRenderer(renderer);
        addColumn(col);

        // column 2 - Name
        col = new TableColumn(2);
        col.setHeaderValue(tr("Name"));
        col.setResizable(true);
        col.setPreferredWidth(200);
        col.setCellRenderer(new OsmPrimitivRenderer());
        addColumn(col);
    }

    public ChangesetContentTableColumnModel() {
        createColumns();
    }
}
