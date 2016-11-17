// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

/**
 * WMTS default layer.
 * @since 11257
 */
public class WMTSDefaultLayer extends DefaultLayer {
    private final String tileMatrixSet;

    /**
     * Constructs a new {@code WMTSDefaultLayer}.
     * @param layerName layer name
     * @param tileMatrixSet tile matrix set
     */
    public WMTSDefaultLayer(String layerName, String tileMatrixSet) {
        super(layerName);
        this.tileMatrixSet = tileMatrixSet;
    }

    /**
     * Returns the tile matrix set.
     * @return the tile matrix set
     */
    public String getTileMatrixSet() {
        return tileMatrixSet;
    }
}
