// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Replace selected relation members with the objects selected in the current dataset
 * @since xxx
 */
public class ReplaceSelectedAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code ReplaceSelectedAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public ReplaceSelectedAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Replace selected members with selected objects"));
        new ImageProvider("dialogs/relation", "replaceselectedright").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        int numSelected = getSelectionTableModel().getRowCount();
        setEnabled(numSelected > 0 &&
                   numSelected == getMemberTableModel().getSelectedIndices().length);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            int[] selectedMemberIndices = getMemberTableModel().getSelectedIndices();
            List<OsmPrimitive> selection = getSelectionTableModel().getSelection();
            int numSelectedPrimitives = selection.size();
            if (numSelectedPrimitives != selectedMemberIndices.length) {
                return;
            }

            List<OsmPrimitive> filteredSelection = filterConfirmedPrimitives(selection, true);

            for (int i = 0; i < selectedMemberIndices.length; i++) {
                getMemberTableModel().updateMemberPrimitive(selectedMemberIndices[i], filteredSelection.get(i));
            }
        } catch (AddAbortException ex) {
            Logging.trace(ex);
        }
    }
}
