// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GeoImageSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, SessionReader.ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for geoimage layer is not supported. Expected: 0.1", version));
        }

        List<ImageEntry> entries = new ArrayList<ImageEntry>();
        NodeList imgNodes = elem.getChildNodes();
        boolean useThumbs = false;
        for (int i=0; i<imgNodes.getLength(); ++i) {
            Node imgNode = imgNodes.item(i);
            if (imgNode.getNodeType() == Node.ELEMENT_NODE) {
                Element imgElem = (Element) imgNode;
                if (imgElem.getTagName().equals("geoimage")) {
                    ImageEntry entry = new ImageEntry();
                    NodeList attrNodes = imgElem.getChildNodes();
                    for (int j=0; j<attrNodes.getLength(); ++j) {
                        Node attrNode = attrNodes.item(j);
                        if (attrNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element attrElem = (Element) attrNode;
                            try {
                                if (attrElem.getTagName().equals("file")) {
                                    entry.setFile(new File(attrElem.getTextContent()));
                                } else if (attrElem.getTagName().equals("position")) {
                                    double lat = Double.parseDouble(attrElem.getAttribute("lat"));
                                    double lon = Double.parseDouble(attrElem.getAttribute("lon"));
                                    entry.setPos(new LatLon(lat, lon));
                                } else if (attrElem.getTagName().equals("speed")) {
                                    entry.setSpeed(Double.parseDouble(attrElem.getTextContent()));
                                } else if (attrElem.getTagName().equals("elevation")) {
                                    entry.setElevation(Double.parseDouble(attrElem.getTextContent()));
                                } else if (attrElem.getTagName().equals("gps-time")) {
                                    entry.setGpsTime(new Date(Long.parseLong(attrElem.getTextContent())));
                                } else if (attrElem.getTagName().equals("exif-orientation")) {
                                    entry.setExifOrientation(Integer.parseInt(attrElem.getTextContent()));
                                } else if (attrElem.getTagName().equals("exif-time")) {
                                    entry.setExifTime(new Date(Long.parseLong(attrElem.getTextContent())));
                                } else if (attrElem.getTagName().equals("exif-gps-time")) {
                                    entry.setExifGpsTime(new Date(Long.parseLong(attrElem.getTextContent())));
                                } else if (attrElem.getTagName().equals("exif-coordinates")) {
                                    double lat = Double.parseDouble(attrElem.getAttribute("lat"));
                                    double lon = Double.parseDouble(attrElem.getAttribute("lon"));
                                    entry.setExifCoor(new LatLon(lat, lon));
                                } else if (attrElem.getTagName().equals("exif-image-direction")) {
                                    entry.setExifImgDir(Double.parseDouble(attrElem.getTextContent()));
                                } else if (attrElem.getTagName().equals("is-new-gps-data") && Boolean.parseBoolean(attrElem.getTextContent())) {
                                    entry.flagNewGpsData();
                                }
                                // TODO: handle thumbnail loading
                            } catch (NumberFormatException e) {
                                // nothing
                            }
                        }
                    }
                    entries.add(entry);
                } else if (imgElem.getTagName().equals("show-thumbnails")) {
                    useThumbs = Boolean.parseBoolean(imgElem.getTextContent());
                }
            }
        }

        GpxLayer gpxLayer = null;
        List<SessionReader.LayerDependency> deps = support.getLayerDependencies();
        if (!deps.isEmpty()) {
            Layer layer = deps.iterator().next().getLayer();
            if (layer instanceof GpxLayer) {
                gpxLayer = (GpxLayer) layer;
            }
        }

        return new GeoImageLayer(entries, gpxLayer, useThumbs);
    }

}
