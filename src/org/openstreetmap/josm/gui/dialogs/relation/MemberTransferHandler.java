// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable;

class MemberTransferHandler extends TransferHandler {

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        final MemberTable source = (MemberTable) c;
        return new RelationMemberTransferable(source.getMemberTableModel().getSelectedMembers());
    }

    @Override
    public boolean canImport(TransferSupport support) {
        support.setShowDropLocation(true);
        return support.isDataFlavorSupported(RelationMemberTransferable.RELATION_MEMBER_DATA);
    }

    @Override
    public boolean importData(TransferSupport support) {
        final int insertRow = ((JTable.DropLocation) support.getDropLocation()).getRow();
        final Collection<RelationMemberData> memberData;
        try {
            //noinspection unchecked
            memberData = (Collection<RelationMemberData>) support.getTransferable().getTransferData(RelationMemberTransferable.RELATION_MEMBER_DATA);
        } catch (UnsupportedFlavorException | IOException e) {
            Main.warn(e);
            return false;
        }
        final MemberTable destination = (MemberTable) support.getComponent();

        try {
            importRelationMemberData(memberData, destination, insertRow);
        } catch (Exception e) {
            Main.warn(e);
            throw e;
        }
        return true;
    }

    protected void importRelationMemberData(Collection<RelationMemberData> memberData, MemberTable destination, int insertRow) {
        final Collection<RelationMember> membersToAdd = new ArrayList<>(memberData.size());
        for (RelationMemberData member : memberData) {
            final OsmPrimitive p = destination.getLayer().data.getPrimitiveById(member.getUniqueId(), member.getType());
            if (p != null) {
                membersToAdd.add(new RelationMember(member.getRole(), p));
            } else {
                Main.warn(tr("Cannot add {0} since it is not part of dataset", member));
            }
        }
        destination.getMemberTableModel().addMembersAtIndex(membersToAdd, insertRow);
    }

    @Override
    protected void exportDone(JComponent sourceComponent, Transferable data, int action) {
        if (action != MOVE) {
            return;
        }
        final MemberTable source = (MemberTable) sourceComponent;
        final MemberTableModel model = source.getMemberTableModel();
        model.remove(source.getSelectedRows());
        model.selectionChanged(null);
    }
}
