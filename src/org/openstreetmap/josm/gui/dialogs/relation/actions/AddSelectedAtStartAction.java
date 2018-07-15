// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Add all objects selected in the current dataset before the first member.
 * @since 9496
 */
public class AddSelectedAtStartAction extends AddFromSelectionAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AddSelectedAtStartAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public AddSelectedAtStartAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset before the first member"));
        new ImageProvider("dialogs/conflict", "copystartright").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getSelectionTableModel().getRowCount() > 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            getMemberTableModel().addMembersAtBeginning(filterConfirmedPrimitives(getSelectionTableModel().getSelection()));
        } catch (AddAbortException ex) {
            Logging.trace(ex);
        }
    }
}
