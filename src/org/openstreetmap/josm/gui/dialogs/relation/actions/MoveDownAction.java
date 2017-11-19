// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Move the currently selected members down.
 * @since 9496
 */
public class MoveDownAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code MoveDownAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey action map key
     */
    public MoveDownAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey) {
        super(memberTable, memberTableModel, actionMapKey);
        new ImageProvider("dialogs", "movedown").getResource().attachImageIcon(this, true);
        Shortcut sc = Shortcut.registerShortcut("relationeditor:movedown", tr("Relation Editor: Move Down"), KeyEvent.VK_DOWN, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Move the currently selected members down"), sc));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.moveDown(memberTable.getSelectedRows());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.canMoveDown(memberTable.getSelectedRows()));
    }
}
