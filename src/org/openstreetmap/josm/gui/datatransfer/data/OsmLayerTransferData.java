// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.data;

import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.LayerTransferable;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * A special form of {@link LayerTransferData} that ensures you that the layer is an OSM data layer
 * @author Michael Zangl
 * @since 10605
 */
public class OsmLayerTransferData extends LayerTransferData {

    /**
     * This is a data flavor specific for OSM data layers.
     * @see LayerTransferData#FLAVOR
     * @see #FLAVORS
     */
    public static final DataFlavor OSM_FLAVOR = ClipboardUtils.newDataFlavor(OsmLayerTransferData.class, "Layer");

    /**
     * The flavors that are supported by this data type.
     */
    public static final List<DataFlavor> FLAVORS = Collections
            .unmodifiableList(Arrays.asList(OSM_FLAVOR, LayerTransferData.FLAVOR, LayerTransferable.LAYER_DATA));

    private final OsmDataLayer osmLayer;

    /**
     * Create a new {@link OsmLayerTransferData} object
     * @param layerManager The layer manager
     * @param layer The layer that is moved.
     */
    public OsmLayerTransferData(LayerManager layerManager, OsmDataLayer layer) {
        super(layerManager, layer);
        osmLayer = layer;
    }

    /**
     * Gets the OSM data layer.
     * @return The layer
     */
    public OsmDataLayer getOsmLayer() {
        return osmLayer;
    }

    @Override
    public List<DataFlavor> getSupportedFlavors() {
        return FLAVORS;
    }

    @Override
    public String toString() {
        return "OsmLayerTransferData [osmLayer=" + osmLayer + ']';
    }
}
