// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Selects  members in the relation editor which refer to primitives in the current selection of the context layer.
 * @since 9496
 */
public class SelectedMembersForSelectionAction extends AddFromSelectionAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code SelectedMembersForSelectionAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public SelectedMembersForSelectionAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to objects in the current selection"));
        new ImageProvider("dialogs/relation", "selectmembers").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        boolean enabled = getSelectionTableModel().getRowCount() > 0
        && !editorAccess.getMemberTableModel().getChildPrimitives(getLayer().data.getSelected()).isEmpty();

        if (enabled) {
            putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to {0} objects in the current selection",
                    editorAccess.getMemberTableModel().getChildPrimitives(getLayer().data.getSelected()).size()));
        } else {
            putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to objects in the current selection"));
        }
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().selectMembersReferringTo(getLayer().data.getSelected());
    }
}
