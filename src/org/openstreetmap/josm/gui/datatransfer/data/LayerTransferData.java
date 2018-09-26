// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.data;

import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.LayerTransferable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;

/**
 * This transferable implements a layer transfer.
 * @author Michael Zangl
 * @since 10605
 */
public class LayerTransferData extends LayerTransferable.Data {
    /**
     * This is a data flavor for all layer types
     */
    public static final DataFlavor FLAVOR = ClipboardUtils.newDataFlavor(LayerTransferData.class, "Layer");

    /**
     * The flavors that are supported by this data type.
     */
    private static final List<DataFlavor> FLAVORS = Arrays.asList(LayerTransferData.FLAVOR, LayerTransferable.LAYER_DATA);

    private final Layer layer;

    /**
     * Create a new transfer data for the given layer
     * @param layerManager The layer manager that the layer is moved in. May be <code>null</code>
     * @param layer The layer
     */
    public LayerTransferData(LayerManager layerManager, Layer layer) {
        super(layerManager, Collections.singletonList(layer));
        this.layer = layer;
    }

    /**
     * Gets the layer to be transferred.
     * @return The layer
     */
    public Layer getLayer() {
        return layer;
    }

    /**
     * Gets a list of flavors supported by this data.
     * @return The flavors.
     */
    public List<DataFlavor> getSupportedFlavors() {
        return Collections.unmodifiableList(FLAVORS);
    }

    @Override
    public String toString() {
        return "LayerTransferData [layer=" + layer + ']';
    }
}
