// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.GBC;
import org.w3c.dom.Element;

public class GeoImageSessionExporter implements SessionLayerExporter {

    private GeoImageLayer layer;

    public GeoImageSessionExporter(GeoImageLayer layer) {
        this.layer = layer;
    }

    private JCheckBox export;

    @Override
    public Collection<Layer> getDependencies() {
        if (layer.getGpxLayer() != null)
            return Collections.singleton((Layer) layer.getGpxLayer());
        else
            return Collections.emptySet();
    }

    @Override
    public Component getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        export = new JCheckBox();
        export.setSelected(true);
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
        lbl.setToolTipText(layer.getToolTipText());
        lbl.setLabelFor(export);
        p.add(export, GBC.std());
        p.add(lbl, GBC.std());
        p.add(GBC.glue(1,0), GBC.std().fill(GBC.HORIZONTAL));
        return p;
    }

    @Override
    public boolean shallExport() {
        return export.isSelected();
    }

    @Override
    public boolean requiresZip() {
        return false;
    }

    @Override
    public Element export(SessionWriter.ExportSupport support) throws IOException {
        Element layerElem = support.createElement("layer");
        layerElem.setAttribute("type", "geoimage");
        layerElem.setAttribute("version", "0.1");
        addAttr("show-thumbnails", Boolean.toString(layer.isUseThumbs()), layerElem, support);

        for (ImageEntry entry : layer.getImages()) {

            Element imgElem = support.createElement("geoimage");

            if (entry.getFile() == null) {
                Main.warn("No file attribute for image - skipping entry");
                break;
            }
            addAttr("file", entry.getFile().getPath(), imgElem, support);
            // FIXME: relative filenames as option
            // FIXME: include images as option (?)

            addAttr("thumbnail", Boolean.toString(entry.hasThumbnail()), imgElem, support);
            if (entry.getPos() != null) {
                Element posElem = support.createElement("position");
                posElem.setAttribute("lat", Double.toString(entry.getPos().lat()));
                posElem.setAttribute("lon", Double.toString(entry.getPos().lon()));
                imgElem.appendChild(posElem);
            }
            if (entry.getSpeed() != null) {
                addAttr("speed", entry.getSpeed().toString(), imgElem, support);
            }
            if (entry.getElevation() != null) {
                addAttr("elevation", entry.getElevation().toString(), imgElem, support);
            }
            if (entry.hasGpsTime()) {
                addAttr("gps-time", Long.toString(entry.getGpsTime().getTime()), imgElem, support);
            }
            if (entry.getExifOrientation() != null) {
                addAttr("exif-orientation", Integer.toString(entry.getExifOrientation()), imgElem, support);
            }
            if (entry.hasExifTime()) {
                addAttr("exif-time", Long.toString(entry.getExifTime().getTime()), imgElem, support);
            }
            if (entry.hasExifGpsTime()) {
                addAttr("exif-gps-time", Long.toString(entry.getExifGpsTime().getTime()), imgElem, support);
            }
            if (entry.getExifCoor() != null) {
                Element posElem = support.createElement("exif-coordinates");
                posElem.setAttribute("lat", Double.toString(entry.getExifCoor().lat()));
                posElem.setAttribute("lon", Double.toString(entry.getExifCoor().lon()));
                imgElem.appendChild(posElem);
            }
            if (entry.getExifImgDir() != null) {
                addAttr("exif-image-direction", entry.getExifImgDir().toString(), imgElem, support);
            }
            if (entry.hasNewGpsData()) {
                addAttr("is-new-gps-data", Boolean.toString(entry.hasNewGpsData()), imgElem, support);
            }

            layerElem.appendChild(imgElem);
        }
        return layerElem;
    }

    private void addAttr(String name, String value, Element element, SessionWriter.ExportSupport support) {
            Element attrElem = support.createElement(name);
            attrElem.appendChild(support.createTextNode(value));
            element.appendChild(attrElem);
    }
}
