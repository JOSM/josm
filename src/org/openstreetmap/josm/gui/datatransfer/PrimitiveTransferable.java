// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.DefaultNameFormatter;

/**
 * Transferable objects for {@link PrimitiveData}.
 * @since 9369
 */
public class PrimitiveTransferable implements Transferable {

    /**
     * Data flavor for {@link PrimitiveData}.
     */
    public static final DataFlavor PRIMITIVE_DATA = new DataFlavor(PrimitiveData.class, PrimitiveData.class.getName());
    private final Collection<? extends OsmPrimitive> primitives;

    /**
     * Constructs a new {@code PrimitiveTransferable}.
     * @param primitives collection of OSM primitives
     */
    public PrimitiveTransferable(Collection<? extends OsmPrimitive> primitives) {
        this.primitives = primitives;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{PRIMITIVE_DATA, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == PRIMITIVE_DATA;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return getStringData();
        } else if (PRIMITIVE_DATA.equals(flavor)) {
            return getRelationMemberData();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    protected String getStringData() {
        final StringBuilder sb = new StringBuilder();
        for (OsmPrimitive primitive : primitives) {
            sb.append(primitive.getType());
            sb.append(" ").append(primitive.getUniqueId());
            sb.append(" # ").append(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
            sb.append("\n");
        }
        return sb.toString().replace("\u200E", "").replace("\u200F", "");
    }

    protected Collection<PrimitiveData> getRelationMemberData() {
        final Collection<PrimitiveData> r = new ArrayList<>(primitives.size());
        for (OsmPrimitive primitive : primitives) {
            r.add(primitive.save());
        }
        return r;
    }
}
