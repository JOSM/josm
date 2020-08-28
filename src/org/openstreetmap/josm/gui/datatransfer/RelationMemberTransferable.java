// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Transferable objects for {@link RelationMemberData}.
 * @since 9368
 */
public class RelationMemberTransferable implements Transferable {

    /**
     * A wrapper for a collection of {@link RelationMemberData}.
     */
    public static final class Data implements Serializable {
        private static final long serialVersionUID = -8432393711635811029L;
        private final Collection<RelationMemberData> relationMemberDatas;

        private Data(Collection<RelationMemberData> primitiveData) {
            CheckParameterUtil.ensureThat(primitiveData instanceof Serializable, "primitiveData must be instanceof Serializable");
            this.relationMemberDatas = primitiveData;
        }

        /**
         * Returns the contained {@link RelationMemberData}
         * @return the contained {@link RelationMemberData}
         */
        public Collection<RelationMemberData> getRelationMemberData() {
            return Collections.unmodifiableCollection(relationMemberDatas);
        }

        /**
         * Gets the Data for the given list of members.
         * @param members The collection. The order is preserved.
         * @return The data.
         */
        public static Data getData(Collection<RelationMember> members) {
            return new Data(members.stream()
                    .map(m -> new RelationMemberData(m.getRole(), m.getType(), m.getUniqueId()))
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Data flavor for {@link RelationMemberData} which is wrapped in {@link Data}.
     */
    public static final DataFlavor RELATION_MEMBER_DATA = new DataFlavor(Data.class, "Relation member");
    private final Collection<RelationMember> members;

    /**
     * Constructs a new {@code RelationMemberTransferable}.
     * @param members list of relation members
     */
    public RelationMemberTransferable(Collection<RelationMember> members) {
        this.members = new ArrayList<>(members);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{RELATION_MEMBER_DATA, PrimitiveTransferData.DATA_FLAVOR, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arrays.asList(getTransferDataFlavors()).contains(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return getStringData();
        } else if (RELATION_MEMBER_DATA.equals(flavor)) {
            return getRelationMemberData();
        } else if (PrimitiveTransferData.DATA_FLAVOR.equals(flavor)) {
            return getPrimitiveData();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    private PrimitiveTransferData getPrimitiveData() {
        Collection<OsmPrimitive> primitives = members.stream().map(RelationMember::getMember).collect(Collectors.toSet());
        return PrimitiveTransferData.getData(primitives);
    }

    protected String getStringData() {
        final StringBuilder sb = new StringBuilder();
        for (RelationMember member : members) {
            sb.append(member.getType())
              .append(' ').append(member.getUniqueId())
              .append(' ').append(member.getRole())
              .append(" # ").append(member.getMember().getDisplayName(DefaultNameFormatter.getInstance()))
              .append('\n');
        }
        return sb.toString().replace("\u200C", "").replace("\u200E", "").replace("\u200F", "");
    }

    protected Data getRelationMemberData() {
        return Data.getData(members);
    }
}
