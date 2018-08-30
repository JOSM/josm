// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Session importer for {@link GeoImageLayer}.
 * @since 5505
 */
public class GeoImageSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, SessionReader.ImportSupport support, ProgressMonitor progressMonitor)
            throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for geoimage layer is not supported. Expected: 0.1", version));
        }

        List<ImageEntry> entries = new ArrayList<>();
        NodeList imgNodes = elem.getChildNodes();
        boolean useThumbs = false;
        for (int i = 0; i < imgNodes.getLength(); ++i) {
            Node imgNode = imgNodes.item(i);
            if (imgNode.getNodeType() == Node.ELEMENT_NODE) {
                Element imgElem = (Element) imgNode;
                if ("geoimage".equals(imgElem.getTagName())) {
                    ImageEntry entry = new ImageEntry();
                    NodeList attrNodes = imgElem.getChildNodes();
                    for (int j = 0; j < attrNodes.getLength(); ++j) {
                        Node attrNode = attrNodes.item(j);
                        if (attrNode.getNodeType() == Node.ELEMENT_NODE) {
                            handleElement(entry, (Element) attrNode);
                        }
                    }
                    entries.add(entry);
                } else if ("show-thumbnails".equals(imgElem.getTagName())) {
                    useThumbs = Boolean.parseBoolean(imgElem.getTextContent());
                }
            }
        }

        GpxLayer gpxLayer = null;
        List<SessionReader.LayerDependency> deps = support.getLayerDependencies();
        if (!deps.isEmpty()) {
            Layer layer = deps.get(0).getLayer();
            if (layer instanceof GpxLayer) {
                gpxLayer = (GpxLayer) layer;
            }
        }

        return new GeoImageLayer(entries, gpxLayer, useThumbs);
    }

    private static void handleElement(GpxImageEntry entry, Element attrElem) {
        try {
            switch(attrElem.getTagName()) {
            case "file":
                entry.setFile(new File(attrElem.getTextContent()));
                break;
            case "position":
                double lat = Double.parseDouble(attrElem.getAttribute("lat"));
                double lon = Double.parseDouble(attrElem.getAttribute("lon"));
                entry.setPos(new LatLon(lat, lon));
                break;
            case "speed":
                entry.setSpeed(Double.valueOf(attrElem.getTextContent()));
                break;
            case "elevation":
                entry.setElevation(Double.valueOf(attrElem.getTextContent()));
                break;
            case "gps-time":
                entry.setGpsTime(new Date(Long.parseLong(attrElem.getTextContent())));
                break;
            case "exif-orientation":
                entry.setExifOrientation(Integer.valueOf(attrElem.getTextContent()));
                break;
            case "exif-time":
                entry.setExifTime(new Date(Long.parseLong(attrElem.getTextContent())));
                break;
            case "exif-gps-time":
                entry.setExifGpsTime(new Date(Long.parseLong(attrElem.getTextContent())));
                break;
            case "exif-coordinates":
                entry.setExifCoor(new LatLon(
                        Double.parseDouble(attrElem.getAttribute("lat")),
                        Double.parseDouble(attrElem.getAttribute("lon"))));
                break;
            case "exif-image-direction":
                entry.setExifImgDir(Double.valueOf(attrElem.getTextContent()));
                break;
            case "is-new-gps-data":
                if (Boolean.parseBoolean(attrElem.getTextContent())) {
                    entry.flagNewGpsData();
                }
                break;
            default: // Do nothing
            }
            // TODO: handle thumbnail loading
        } catch (NumberFormatException e) {
            Logging.trace(e);
        }
    }
}
