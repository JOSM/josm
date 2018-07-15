// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Remove all members referring to one of the selected objects.
 * @since 9496
 */
public class RemoveSelectedAction extends AddFromSelectionAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code RemoveSelectedAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public RemoveSelectedAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.SELECTION_TABLE_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Remove all members referring to one of the selected objects"));
        new ImageProvider("dialogs/relation", "deletemembers").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayer().data;
        if (ds == null || ds.selectionEmpty()) {
            setEnabled(false);
            return;
        }
        // only enable the action if we have members referring to the selected primitives
        setEnabled(editorAccess.getMemberTableModel().hasMembersReferringTo(ds.getSelected()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().removeMembersReferringTo(getSelectionTableModel().getSelection());
    }
}
