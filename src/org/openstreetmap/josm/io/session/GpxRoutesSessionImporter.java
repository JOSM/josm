// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * Session importer for {@link org.openstreetmap.josm.gui.layer.GpxRouteLayer}.
 */
public class GpxRoutesSessionImporter extends GpxTracksSessionImporter {

    @Override
    protected Layer getLayer(GpxImporter.GpxImporterData importData) {
        return importData.getGpxRouteLayer();
    }
}
