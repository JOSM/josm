// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.gui.layer.TMSLayer;

/**
 * JOSM wrapper class that uses min/max zoom settings from imagery settings instead of JMapViewer defaults
 * @since 15456
 */
public class JosmTemplatedTMSTileSource extends TemplatedTMSTileSource {

    /**
     * Creates TMS tilesource based on tilesource info
     * @param info tile source info
     */
    public JosmTemplatedTMSTileSource(TileSourceInfo info) {
        super(info);
    }

    @Override
    public int getMinZoom() {
        return (minZoom == 0) ? TMSLayer.PROP_MIN_ZOOM_LVL.get() : minZoom;
    }

    // return no more, than JMapViewer supports
    @Override
    public int getMaxZoom() {
        return Math.min((maxZoom == 0) ? TMSLayer.PROP_MAX_ZOOM_LVL.get() : maxZoom, JMapViewer.MAX_ZOOM);
    }
}
