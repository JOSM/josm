// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Sort the selected relation members and all members below.
 * @since 9496
 */
public class SortBelowAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code SortBelowAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     */
    public SortBelowAction(MemberTable memberTable, MemberTableModel memberTableModel) {
        super(memberTable, memberTableModel, null);
        new ImageProvider("dialogs", "sort_below").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Sort below"));
        putValue(SHORT_DESCRIPTION, tr("Sort the selected relation members and all members below"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.sortBelow();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.getRowCount() > 0 && !memberTableModel.getSelectionModel().isSelectionEmpty());
    }
}
