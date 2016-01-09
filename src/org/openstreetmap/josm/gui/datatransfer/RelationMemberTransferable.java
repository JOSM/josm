// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.DefaultNameFormatter;

public class RelationMemberTransferable implements Transferable {

    public static final DataFlavor RELATION_MEMBER_DATA = new DataFlavor(RelationMemberData.class, RelationMemberData.class.getName());
    private final Collection<RelationMember> members;

    public RelationMemberTransferable(Collection<RelationMember> members) {
        this.members = members;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{RELATION_MEMBER_DATA, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == RELATION_MEMBER_DATA;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return getStringData();
        } else if (RELATION_MEMBER_DATA.equals(flavor)) {
            return getRelationMemberData();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    protected String getStringData() {
        final StringBuilder sb = new StringBuilder();
        for (RelationMember member : members) {
            sb.append(member.getType());
            sb.append(" ").append(member.getUniqueId());
            sb.append(" ").append(member.getRole());
            sb.append(" # ").append(member.getMember().getDisplayName(DefaultNameFormatter.getInstance()));
            sb.append("\n");
        }
        return sb.toString().replace("\u200E", "").replace("\u200F", "");
    }

    protected Collection<RelationMemberData> getRelationMemberData() {
        final Collection<RelationMemberData> r = new ArrayList<>(members.size());
        for (RelationMember member : members) {
            r.add(new RelationMemberData(member.getRole(), member.getType(), member.getUniqueId()));
        }
        return r;
    }
}
