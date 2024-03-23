// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.w3c.dom.Element;

/**
 * Session importer for {@link MarkerLayer}.
 * @since 5684
 */
public class MarkerSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for marker layer is not supported. Expected: 0.1", version));
        }
        String fileStr = OsmDataSessionImporter.extractFileName(elem, support);

        try (InputStream in = support.getInputStream(fileStr)) {
            GpxImporter.GpxImporterData importData = GpxImporter.loadLayers(in, support.getFile(fileStr), support.getLayerName(),
                    progressMonitor);

            support.addPostLayersTask(importData.getPostLayerTask());

            importData.getGpxLayer().destroy();
            return importData.getMarkerLayer();
        }
    }
}
