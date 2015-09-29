// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.w3c.dom.Element;

/**
 * Session exporter for TMSLayer and WMSLayer.
 */
public class ImagerySessionExporter implements SessionLayerExporter {

    private ImageryLayer layer;
    private JCheckBox export;

    public ImagerySessionExporter(ImageryLayer layer) {
        this.layer = layer;
    }

    public ImagerySessionExporter(TMSLayer layer) {
        this((ImageryLayer) layer);
    }

    public ImagerySessionExporter(WMSLayer layer) {
        this((ImageryLayer) layer);
    }

    @Override
    public Collection<Layer> getDependencies() {
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
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
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
    public Element export(ExportSupport support) throws IOException {
        Element layerElem = support.createElement("layer");
        layerElem.setAttribute("type", "imagery");
        layerElem.setAttribute("version", "0.1");
        ImageryPreferenceEntry e = new ImageryPreferenceEntry(layer.getInfo());
        Map<String, String> data = new LinkedHashMap<>(Preferences.serializeStruct(e, ImageryPreferenceEntry.class));
        if (layer instanceof AbstractTileSourceLayer) {
            AbstractTileSourceLayer tsLayer = (AbstractTileSourceLayer) layer;
            data.put("automatic-downloading", Boolean.toString(tsLayer.autoLoad));
            data.put("automatically-change-resolution", Boolean.toString(tsLayer.autoZoom));
            data.put("show-errors", Boolean.toString(tsLayer.showErrors));
        }
        data.put("dx", String.valueOf(layer.getDx()));
        data.put("dy", String.valueOf(layer.getDy()));
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Element attrElem = support.createElement(entry.getKey());
            layerElem.appendChild(attrElem);
            attrElem.appendChild(support.createTextNode(entry.getValue()));
        }
        return layerElem;
    }

}
