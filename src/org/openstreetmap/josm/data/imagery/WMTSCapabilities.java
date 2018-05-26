// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.Collection;

import org.openstreetmap.josm.data.imagery.GetCapabilitiesParseHelper.TransferMode;
import org.openstreetmap.josm.data.imagery.WMTSTileSource.Layer;

/**
 * Data object containing WMTS GetCapabilities document
 *
 * @author Wiktor Niesiobedzki
 * @since 13733
 */
public class WMTSCapabilities {
    private final String baseUrl;
    private final TransferMode transferMode;
    private Collection<Layer> layers;

    /**
     * Constructor for the class
     *
     * @param baseUrl of this service
     * @param transferMode either KVP (key-value pairs in URL parameters) or RESTful (part of path)
     */
    public WMTSCapabilities(String baseUrl, TransferMode transferMode) {
        this.baseUrl = baseUrl;
        this.transferMode = transferMode;
    }

    /**
     * Adds layers to this WMTS service response
     * @param layers layers to add to this document
     */
    public void addLayers(Collection<Layer> layers) {
        this.layers = layers;
    }

    /**
     * Layers of this WMTS service
     * @return layers defined by this service
     */
    public Collection<Layer> getLayers() {
        return layers;
    }

    /**
     * base URL of this WMTS service
     * @return base url for this service
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * TransferMode for this service
     * @return transfer mode (KVP or RESTful) for this service
     */
    public TransferMode getTransferMode() {
        return transferMode;
    }
}
