// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Add all objects selected in the current dataset after the last member.
 * @since 9496
 */
public class AddSelectedAtEndAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code AddSelectedAtEndAction}.
     * @param memberTableModel member table model
     * @param selectionTableModel selection table model
     * @param editor relation editor
     */
    public AddSelectedAtEndAction(MemberTableModel memberTableModel, SelectionTableModel selectionTableModel, IRelationEditor editor) {
        super(null, memberTableModel, null, selectionTableModel, null, null, editor);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last member"));
        new ImageProvider("dialogs/conflict", "copyendright").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(selectionTableModel.getRowCount() > 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            memberTableModel.addMembersAtEnd(filterConfirmedPrimitives(selectionTableModel.getSelection()));
        } catch (AddAbortException ex) {
            Logging.trace(ex);
        }
    }
}
