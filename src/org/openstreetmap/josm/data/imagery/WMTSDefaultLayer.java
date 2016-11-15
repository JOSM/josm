// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

public class WMTSDefaultLayer extends DefaultLayer {
    String tileMatrixSet;

    public WMTSDefaultLayer(String layerName, String tileMatrixSet) {
        super(layerName);
        this.tileMatrixSet = tileMatrixSet;
    }

    public String getTileMatrixSet() {
        return tileMatrixSet;
    }
}
