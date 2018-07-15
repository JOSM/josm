// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Sort the selected relation members and all members below.
 * @since 9496
 */
public class SortBelowAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code SortBelowAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     */
    public SortBelowAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        new ImageProvider("dialogs", "sort_below").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Sort below"));
        putValue(SHORT_DESCRIPTION, tr("Sort the selected relation members and all members below"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().sortBelow();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().getRowCount() > 0 && !editorAccess.getMemberTableModel().getSelectionModel().isSelectionEmpty());
    }
    
    @Override
    public boolean isExpertOnly() {
        return true;
    }
}
