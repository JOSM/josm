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
    private boolean keepCopiedMembers = true;

    /**
     * Constructs a new {@code CopyMembersAction}.
     * @param editorAccess An interface to access the relation editor contents.
     * @param keepCopiedMembers if true, copied members are kept in the table; otherwise they are removed (cut)
     */
    public CopyMembersAction(IRelationEditorActionAccess editorAccess, boolean keepCopiedMembers) {
        super(editorAccess);
        this.keepCopiedMembers = keepCopiedMembers;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Collection<RelationMember> members = getMemberTableModel().getSelectedMembers();

        if (!members.isEmpty()) {
            ClipboardUtils.copy(new RelationMemberTransferable(members));
            if (!this.keepCopiedMembers) {
                getMemberTableModel().remove(getMemberTableModel().getSelectedIndices());
            }
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
