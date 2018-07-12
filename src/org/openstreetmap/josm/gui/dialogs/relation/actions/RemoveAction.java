// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Remove the currently selected members from this relation.
 * @since 9496
 */
public class RemoveAction extends AbstractRelationEditorAction {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code RemoveAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param actionMapKey action map key
     */
    public RemoveAction(IRelationEditorActionAccess editorAccess, String actionMapKey) {
        super(editorAccess, actionMapKey, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Remove"));
        Shortcut sc = Shortcut.registerShortcut("relationeditor:remove", tr("Relation Editor: Remove"), KeyEvent.VK_DELETE, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Remove the currently selected members from this relation"), sc));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().remove(editorAccess.getMemberTable().getSelectedRows());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().canRemove(editorAccess.getMemberTable().getSelectedRows()));
    }
}
