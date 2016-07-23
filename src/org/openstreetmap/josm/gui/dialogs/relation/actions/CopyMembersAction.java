// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Copy members.
 * @since 9496
 */
public class CopyMembersAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code CopyMembersAction}.
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public CopyMembersAction(MemberTableModel memberTableModel, OsmDataLayer layer, IRelationEditor editor) {
        super(null, memberTableModel, null, null, null, layer, editor);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Collection<RelationMember> members = memberTableModel.getSelectedMembers();

        if (!members.isEmpty()) {
            ClipboardUtils.copy(new RelationMemberTransferable(members));
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
