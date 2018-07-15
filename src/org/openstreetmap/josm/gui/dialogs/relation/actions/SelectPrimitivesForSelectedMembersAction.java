// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Selects primitives in the layer this editor belongs to. The selected primitives are
 * equal to the set of primitives the currently selected relation members refer to.
 * @since 9496
 */
public class SelectPrimitivesForSelectedMembersAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Select objects for selected relation members.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer layer
     */
    public SelectPrimitivesForSelectedMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(SHORT_DESCRIPTION, tr("Select objects for selected relation members"));
        new ImageProvider("dialogs/relation", "selectprimitives").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTable().getSelectedRowCount() > 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getLayer().data.setSelected(editorAccess.getMemberTableModel().getSelectedChildPrimitives());
    }
}
