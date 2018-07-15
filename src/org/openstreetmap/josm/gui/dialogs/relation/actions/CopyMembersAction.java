// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable;

/**
 * Copy members.
 * @since 9496
 */
public class CopyMembersAction extends AddFromSelectionAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code CopyMembersAction}.
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public CopyMembersAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Collection<RelationMember> members = getMemberTableModel().getSelectedMembers();

        if (!members.isEmpty()) {
            ClipboardUtils.copy(new RelationMemberTransferable(members));
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
