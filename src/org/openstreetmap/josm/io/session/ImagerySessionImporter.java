// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Session importer for {@link TMSLayer}, {@link WMSLayer} and {@link WMTSLayer}.
 * @since 5391
 */
public class ImagerySessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for imagery layer is not supported. Expected: 0.1", version));
        }
        Map<String, String> attributes = new HashMap<>();

        NodeList nodes = elem.getChildNodes();

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                attributes.put(e.getTagName(), e.getTextContent());
            }
        }
        ImageryPreferenceEntry prefEntry = Preferences.deserializeStruct(attributes, ImageryPreferenceEntry.class);
        ImageryInfo i = new ImageryInfo(prefEntry);
        ImageryLayer layer = ImageryLayer.create(i);
        if (layer instanceof AbstractTileSourceLayer) {
            AbstractTileSourceLayer<?> tsLayer = (AbstractTileSourceLayer<?>) layer;
            tsLayer.getDisplaySettings().loadFrom(attributes);
        }
        if (attributes.containsKey("dx") && attributes.containsKey("dy")) {
            layer.setOffset(Double.parseDouble(attributes.get("dx")), Double.parseDouble(attributes.get("dy")));
        }
        return layer;
    }
}
