// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;

/**
 * This is the column model for the {@link MemberTable}
 */
public class MemberTableColumnModel extends DefaultTableColumnModel {

    /**
     * Constructs a new {@code MemberTableColumnModel}.
     * @param autoCompletionManager the auto completion manager. Must not be null
     * @param relation the relation. Can be null
     * @since 13675
     */
    public MemberTableColumnModel(AutoCompletionManager autoCompletionManager, Relation relation) {
        TableColumn col = null;

        // column 0 - the member role
        col = new TableColumn(0);
        col.setHeaderValue(tr("Role"));
        col.setResizable(true);
        col.setPreferredWidth(100);
        col.setCellRenderer(new MemberTableRoleCellRenderer());
        col.setCellEditor(new MemberRoleCellEditor(autoCompletionManager, relation));
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
