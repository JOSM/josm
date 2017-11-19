// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Reverse the order of the relation members.
 * @since 9496
 */
public class ReverseAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code ReverseAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     */
    public ReverseAction(MemberTable memberTable, MemberTableModel memberTableModel) {
        super(memberTable, memberTableModel, null);
        putValue(SHORT_DESCRIPTION, tr("Reverse the order of the relation members"));
        new ImageProvider("dialogs/relation", "reverse").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Reverse"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.reverse();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.getRowCount() > 0);
    }
}
