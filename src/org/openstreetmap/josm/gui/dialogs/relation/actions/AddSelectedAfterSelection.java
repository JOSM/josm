// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Add all objects selected in the current dataset after the last selected member.
 * @since 9496
 */
public class AddSelectedAfterSelection extends AddFromSelectionAction {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AddSelectedAfterSelection}.
     * @param memberTableModel member table model
     * @param selectionTableModel selection table model
     * @param editor relation editor
     */
    public AddSelectedAfterSelection(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last selected member"));
        new ImageProvider("dialogs/conflict", "copyaftercurrentright").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
		setEnabled(getSelectionTableModel().getRowCount() > 0
				&& getMemberTableModel().getSelectionModel().getMinSelectionIndex() >= 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
        	getMemberTableModel().addMembersAfterIdx(filterConfirmedPrimitives(getSelectionTableModel().getSelection()),
        			getMemberTableModel().getSelectionModel().getMaxSelectionIndex());
        } catch (AddAbortException ex) {
            Logging.trace(ex);
        }
    }
}
