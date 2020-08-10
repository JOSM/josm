// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import org.openstreetmap.josm.gui.layer.GpxRouteLayer;

/**
 * Session exporter for {@link org.openstreetmap.josm.gui.layer.GpxRouteLayer}.
 */
public class GpxRoutesSessionExporter extends GpxTracksSessionExporter {

    /**
     * Constructs a new {@code GpxRoutesSessionExporter}.
     * @param layer GPX route layer to export
     */
    public GpxRoutesSessionExporter(GpxRouteLayer layer) {
        super(layer.fromLayer, "routes");
    }
}
