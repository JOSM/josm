// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import org.openstreetmap.josm.gui.datatransfer.data.LayerTransferData;
import org.openstreetmap.josm.gui.datatransfer.data.OsmLayerTransferData;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This class handles the transfer of a layer inside this JOSM instance.
 * @author Michael Zangl
 * @since 10605
 */
public class SingleLayerTransferable implements Transferable {
    private final LayerTransferData data;

    /**
     * Create a new {@link SingleLayerTransferable}
     * @param manager The manager the layer belongs to
     * @param layer The layer that is transfered.
     */
    public SingleLayerTransferable(LayerManager manager, Layer layer) {
        if (layer instanceof OsmDataLayer) {
            this.data = new OsmLayerTransferData(manager, (OsmDataLayer) layer);
        } else {
            this.data = new LayerTransferData(manager, layer);
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        List<DataFlavor> flavors = data.getSupportedFlavors();
        return flavors.toArray(new DataFlavor[flavors.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return data.getSupportedFlavors().contains(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (isDataFlavorSupported(flavor)) {
            return data;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
