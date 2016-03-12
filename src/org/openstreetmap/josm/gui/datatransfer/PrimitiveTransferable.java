// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Transferable objects for {@link PrimitiveData}.
 * @since 9369
 */
public class PrimitiveTransferable implements Transferable {

    /**
     * A wrapper for a collection of {@link PrimitiveData}.
     */
    public static final class Data implements Serializable {
        private static final long serialVersionUID = -1485089993600213704L;
        private final Collection<PrimitiveData> primitiveData;

        private Data(Collection<PrimitiveData> primitiveData) {
            CheckParameterUtil.ensureThat(primitiveData instanceof Serializable, "primitiveData must be instanceof Serializable");
            this.primitiveData = primitiveData;
        }

        /**
         * Returns the contained {@link PrimitiveData}
         * @return the contained {@link PrimitiveData}
         */
        public Collection<PrimitiveData> getPrimitiveData() {
            return primitiveData;
        }
    }

    /**
     * Data flavor for {@link PrimitiveData} which is wrapped in {@link Data}.
     */
    public static final DataFlavor PRIMITIVE_DATA = new DataFlavor(Data.class, Data.class.getName());
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
            return getPrimitiveData();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    protected String getStringData() {
        final StringBuilder sb = new StringBuilder();
        for (OsmPrimitive primitive : primitives) {
            sb.append(primitive.getType())
              .append(' ').append(primitive.getUniqueId())
              .append(" # ").append(primitive.getDisplayName(DefaultNameFormatter.getInstance()))
              .append('\n');
        }
        return sb.toString().replace("\u200E", "").replace("\u200F", "");
    }

    protected Data getPrimitiveData() {
        final Collection<PrimitiveData> r = new ArrayList<>(primitives.size());
        for (OsmPrimitive primitive : primitives) {
            r.add(primitive.save());
        }
        return new Data(r);
    }
}
