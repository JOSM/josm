// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;

/**
 * This class allows to transfer multiple layers in the current JOSM instance.
 * @author Michael Zangl
 * @since 10605
 */
public class LayerTransferable implements Transferable {

    /**
     * A wrapper for a collection of {@link Layer}.
     */
    public static class Data {
        private final LayerManager manager;
        private final List<Layer> layers;

        /**
         * Create a new data object
         * @param manager The layer manager the layers are from.
         * @param layers The layers.
         */
        public Data(LayerManager manager, List<Layer> layers) {
            super();
            this.manager = manager;
            this.layers = new ArrayList<>(layers);
        }

        /**
         * Gets the layer manager the layers belong to.
         * @return The layer manager. It may be <code>null</code>
         */
        public LayerManager getManager() {
            return manager;
        }

        /**
         * Gets the list of layers that were copied.
         * @return The layers.
         */
        public List<Layer> getLayers() {
            return Collections.unmodifiableList(layers);
        }

        @Override
        public String toString() {
            return "Data [layers=" + layers + ']';
        }
    }

    /**
     * Data flavor for {@link Layer}s which are wrapped in {@link Data}.
     */
    public static final DataFlavor LAYER_DATA = ClipboardUtils.newDataFlavor(Data.class, "Layers");

    private final Data data;

    /**
     * Create a new data object
     * @param manager The layer manager the layers are from.
     * @param layers The layers.
     */
    public LayerTransferable(LayerManager manager, List<Layer> layers) {
        this.data = new Data(manager, layers);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {LAYER_DATA};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return LAYER_DATA.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        } else {
            return data;
        }
    }
}
