// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Reverse the order of the relation members.
 * @since 9496
 */
public class ReverseAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ReverseAction}.
     * @param editorAccess
     */
    public ReverseAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE);

        putValue(SHORT_DESCRIPTION, tr("Reverse the order of the relation members"));
        new ImageProvider("dialogs/relation", "reverse").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Reverse"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().reverse();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().getRowCount() > 0);
    }
}
