// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.io.importexport.NMEAImporter;
import org.openstreetmap.josm.gui.io.importexport.RtkLibImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Session exporter for {@link GpxLayer}.
 * @since 5501
 */
public class GpxTracksSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, SessionReader.ImportSupport support, ProgressMonitor progressMonitor)
            throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for gpx track layer is not supported. Expected: 0.1", version));
        }
        String fileStr = OsmDataSessionImporter.extractFileName(elem, support);

        try (InputStream in = support.getInputStream(fileStr)) {
            GpxImporter.GpxImporterData importData;

            if (NMEAImporter.FILE_FILTER.acceptName(fileStr)) {
                importData = NMEAImporter.loadLayers(in, support.getFile(fileStr), support.getLayerName());
            } else if (RtkLibImporter.FILE_FILTER.acceptName(fileStr)) {
                importData = RtkLibImporter.loadLayers(in, support.getFile(fileStr), support.getLayerName());
            } else {
                importData = GpxImporter.loadLayers(in, support.getFile(fileStr), support.getLayerName(), progressMonitor);
            }
            if (importData.getGpxLayer() != null && importData.getGpxLayer().data != null) {
                importData.getGpxLayer().data.fromSession = true;
            }
            NodeList markerNodes = elem.getElementsByTagName("markerLayer");
            if (markerNodes.getLength() > 0 && markerNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element markerEl = (Element) markerNodes.item(0);
                try {
                    int index = Integer.parseInt(markerEl.getAttribute("index"));
                    support.addSubLayer(index, importData.getMarkerLayer(), markerEl);
                } catch (NumberFormatException ex) {
                    Logging.warn(ex);
                }
            }

            support.addPostLayersTask(importData.getPostLayerTask());
            return getLayer(importData);
        }
    }

    protected Layer getLayer(GpxImporter.GpxImporterData importData) {
        return importData.getGpxLayer();
    }
}
