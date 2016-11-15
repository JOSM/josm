// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

/**
 *
 * Simple class representing default layer that might be set in imagery information
 *
 * This simple class is needed - as for WMS there is different information needed to specify layer than for WMTS
 *
 * @author Wiktor Niesiobedzki
 *
 */
public class DefaultLayer {

    protected String layerName;

    /**
     * Constructor
     * @param layerName that is the DefaultLayer
     */
    public DefaultLayer(String layerName) {
        this.layerName = layerName;
    }

    /**
     * @return layer name of the default layer
     */
    public String getLayerName() {
        return layerName;
    }

}
