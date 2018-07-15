// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Add all objects selected in the current dataset before the first selected member.
 * @since 9496
 */
public class AddSelectedBeforeSelection extends AddFromSelectionAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AddSelectedBeforeSelection}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public AddSelectedBeforeSelection(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset before the first selected member"));
        new ImageProvider("dialogs/conflict", "copybeforecurrentright").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getSelectionTableModel().getRowCount() > 0
                && editorAccess.getMemberTableModel().getSelectionModel().getMinSelectionIndex() >= 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            editorAccess.getMemberTableModel().addMembersBeforeIdx(filterConfirmedPrimitives(getSelectionTableModel().getSelection()),
                    editorAccess.getMemberTableModel().getSelectionModel().getMinSelectionIndex());
        } catch (AddAbortException ex) {
            Logging.trace(ex);
        }
    }
}
