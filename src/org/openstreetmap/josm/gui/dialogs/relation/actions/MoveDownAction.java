// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Move the currently selected members down.
 * @since 9496
 */
public class MoveDownAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MoveDownAction}.
     * @param editorAccess An interface to access the relation editor contents.
     * @param actionMapKey action map key
     */
    public MoveDownAction(IRelationEditorActionAccess editorAccess, String actionMapKey) {
        super(editorAccess, actionMapKey, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        new ImageProvider("dialogs", "down").getResource().attachImageIcon(this, true);
        Shortcut sc = Shortcut.registerShortcut("relationeditor:movedown", tr("Relation Editor: Move Down"), KeyEvent.VK_DOWN, Shortcut.ALT);
        sc.setAccelerator(this);
        sc.setTooltip(this, tr("Move the currently selected members down"));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().moveDown(editorAccess.getMemberTable().getSelectedRows());
        editorAccess.stopMemberCellEditing();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().canMoveDown(editorAccess.getMemberTable().getSelectedRows()));
    }
}
