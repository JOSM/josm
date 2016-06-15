// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * This is the {@link TableColumnModel} used in {@link SelectionTable}.
 * @since 1790
 */
public class SelectionTableColumnModel extends DefaultTableColumnModel {

    /**
     * Constructs a new {@code SelectionTableColumnModel}.
     * @param model member table model
     */
    public SelectionTableColumnModel(MemberTableModel model) {
        // column 0 - the member role
        TableColumn col = new TableColumn(0);
        col.setHeaderValue(tr("Selection"));
        col.setMinWidth(200);
        col.setCellRenderer(new SelectionTableCellRenderer(model));
        addColumn(col);
    }
}
