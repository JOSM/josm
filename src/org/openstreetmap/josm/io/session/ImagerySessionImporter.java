// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
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
        Map<String, String> attributes = readProperties(elem);

        ImageryPreferenceEntry prefEntry = StructUtils.deserializeStruct(attributes, ImageryPreferenceEntry.class);
        ImageryInfo info = new ImageryInfo(prefEntry);
        ImageryLayer layer = ImageryLayer.create(info);
        Utils.instanceOfThen(layer, AbstractTileSourceLayer.class, tsLayer -> {
            tsLayer.getDisplaySettings().applyFromPropertiesMap(attributes);
            if (!tsLayer.getDisplaySettings().isAutoZoom()) {
                String zoomStr = attributes.get("zoom-level");
                if (zoomStr != null) {
                    support.addPostLayersTask(() -> {
                        try {
                            tsLayer.setZoomLevel(Integer.parseInt(zoomStr));
                        } catch (NumberFormatException e) {
                            Logging.warn(e);
                        }
                    });
                }
            }
            Element offsetEl = getFirstElementByTagName(elem, "offset");
            if (offsetEl != null) {
                Map<String, String> offsetAttributes = readProperties(offsetEl);
                OffsetBookmark offset = OffsetBookmark.fromPropertiesMap(offsetAttributes);
                tsLayer.getDisplaySettings().setOffsetBookmark(offset);
            }
        });
        Element filtersEl = getFirstElementByTagName(elem, "filters");
        if (filtersEl != null) {
            ImageryFilterSettings filterSettings = layer.getFilterSettings();
            if (filterSettings != null) {
                Map<String, String> filtersProps = readProperties(filtersEl);
                filterSettings.getProcessors().stream()
                        .flatMap(Utils.castToStream(SessionAwareReadApply.class))
                        .forEach(proc -> proc.applyFromPropertiesMap(filtersProps));
            }
        }
        return layer;
    }

    private static Element getFirstElementByTagName(Element el, String name) {
        NodeList nl = el.getElementsByTagName(name);
        if (nl.getLength() == 0)
            return null;
        return (Element) nl.item(0);
    }

    private static Map<String, String> readProperties(Element elem) {
        NodeList nodes = elem.getChildNodes();
        return IntStream.range(0, nodes.getLength()).mapToObj(nodes::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE && node.getChildNodes().getLength() <= 1)
                .map(node -> (Element) node)
                .collect(Collectors.toMap(Element::getTagName, Node::getTextContent, (a, b) -> b));
    }
}
