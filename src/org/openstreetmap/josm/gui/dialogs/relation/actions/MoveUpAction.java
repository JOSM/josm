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
 * Move the currently selected members up.
 * @since 9496
 */
public class MoveUpAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code MoveUpAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey key in table action map
     */
    public MoveUpAction(MemberTable memberTable, MemberTableModel memberTableModel, String actionMapKey) {
        super(memberTable, memberTableModel, actionMapKey);
        new ImageProvider("dialogs", "moveup").getResource().attachImageIcon(this, true);
        Shortcut sc = Shortcut.registerShortcut("relationeditor:moveup", tr("Relation Editor: Move Up"), KeyEvent.VK_UP, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Move the currently selected members up"), sc));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.moveUp(memberTable.getSelectedRows());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.canMoveUp(memberTable.getSelectedRows()));
    }
}
