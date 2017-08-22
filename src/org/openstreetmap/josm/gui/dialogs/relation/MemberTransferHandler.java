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

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.tools.Logging;

/**
 * A transfer handler that helps with importing / exporting members for relations.
 * @author Michael Zangl
 * @since 10604
 */
public class MemberTransferHandler extends TransferHandler {

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
        if (support.isDrop()) {
            support.setShowDropLocation(true);
        }
        return support.isDataFlavorSupported(RelationMemberTransferable.RELATION_MEMBER_DATA)
                || support.isDataFlavorSupported(PrimitiveTransferData.DATA_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        MemberTable destination = (MemberTable) support.getComponent();
        int insertRow = computeInsertionRow(support, destination);

        return importDataAt(support, destination, insertRow);
    }

    private static int computeInsertionRow(TransferSupport support, MemberTable destination) {
        final int insertRow;
        if (support.isDrop()) {
            DropLocation dl = support.getDropLocation();
            if (dl instanceof JTable.DropLocation) {
                insertRow = ((JTable.DropLocation) dl).getRow();
            } else {
                insertRow = 0;
            }
        } else {
            int selection = destination.getSelectedRow();
            if (selection < 0) {
                // no selection, add at the end.
                insertRow = destination.getRowCount();
            } else {
                insertRow = selection;
            }
        }
        return insertRow;
    }

    private boolean importDataAt(TransferSupport support, MemberTable destination, int insertRow) {
        try {
            if (support.isDataFlavorSupported(RelationMemberTransferable.RELATION_MEMBER_DATA)) {
                importRelationMemberData(support, destination, insertRow);
                return true;
            } else if (support.isDataFlavorSupported(PrimitiveTransferData.DATA_FLAVOR)) {
                importPrimitiveData(support, destination, insertRow);
                return true;
            } else {
                return false;
            }
        } catch (IOException | UnsupportedFlavorException e) {
            Logging.warn(e);
            return false;
        }
    }

    protected void importRelationMemberData(TransferSupport support, final MemberTable destination, int insertRow)
            throws UnsupportedFlavorException, IOException {
        final RelationMemberTransferable.Data memberData = (RelationMemberTransferable.Data)
                support.getTransferable().getTransferData(RelationMemberTransferable.RELATION_MEMBER_DATA);
        importData(destination, insertRow, memberData.getRelationMemberData(), new AbstractRelationMemberConverter<RelationMemberData>() {
            @Override
            protected RelationMember getMember(MemberTable destination, RelationMemberData data, OsmPrimitive p) {
                return new RelationMember(data.getRole(), p);
            }
        });
    }

    protected void importPrimitiveData(TransferSupport support, final MemberTable destination, int insertRow)
            throws UnsupportedFlavorException, IOException {
        final PrimitiveTransferData data = (PrimitiveTransferData)
                support.getTransferable().getTransferData(PrimitiveTransferData.DATA_FLAVOR);
        importData(destination, insertRow, data.getDirectlyAdded(), new AbstractRelationMemberConverter<PrimitiveData>() {
            @Override
            protected RelationMember getMember(MemberTable destination, PrimitiveData data, OsmPrimitive p) {
                return destination.getMemberTableModel().getRelationMemberForPrimitive(p);
            }
        });
    }

    protected <T extends PrimitiveId> void importData(MemberTable destination, int insertRow,
                                  Collection<T> memberData, AbstractRelationMemberConverter<T> toMemberFunction) {
        final Collection<RelationMember> membersToAdd = new ArrayList<>(memberData.size());
        for (T data : memberData) {
            final RelationMember member = toMemberFunction.importPrimitive(destination, data);
            if (member != null) {
                membersToAdd.add(member);
            }
        }
        destination.getMemberTableModel().addMembersAtIndexKeepingOldSelection(membersToAdd, insertRow);
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

    private abstract static class AbstractRelationMemberConverter<T extends PrimitiveId> {
        protected RelationMember importPrimitive(MemberTable destination, T data) {
            final OsmPrimitive p = destination.getLayer().data.getPrimitiveById(data);
            if (p == null) {
                Logging.warn(tr("Cannot add {0} since it is not part of dataset", data));
                return null;
            } else {
                return getMember(destination, data, p);
            }
        }

        protected abstract RelationMember getMember(MemberTable destination, T data, OsmPrimitive p);
    }
}
