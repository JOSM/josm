// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

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
     * @param editorAccess An interface to access the relation editor contents.
     * @param actionMapKey action map key
     */
    public RemoveAction(IRelationEditorActionAccess editorAccess, String actionMapKey) {
        super(editorAccess, actionMapKey, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Remove"));
        Shortcut sc = Shortcut.registerShortcut("relationeditor:remove", tr("Relation Editor: Remove"), KeyEvent.VK_DELETE, Shortcut.ALT);
        sc.setAccelerator(this);
        sc.setTooltip(this, tr("Remove the currently selected members from this relation"));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[] selectedRows = editorAccess.getMemberTable().getSelectedRows();
        editorAccess.getMemberTableModel().remove(selectedRows);
        if (selectedRows.length > 0 && editorAccess.getMemberTableModel().getRowCount() > selectedRows[0]) {
            // make first row of former selection visible, see #17952
            editorAccess.getMemberTable().makeMemberVisible(selectedRows[0]);
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().canRemove(editorAccess.getMemberTable().getSelectedRows()));
    }
}
