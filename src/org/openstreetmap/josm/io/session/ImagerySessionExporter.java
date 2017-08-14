// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.layer.WMTSLayer;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Element;

/**
 * Session exporter for {@link TMSLayer}, {@link WMSLayer} and {@link WMTSLayer}.
 * @since 5391
 */
public class ImagerySessionExporter extends AbstractSessionExporter<ImageryLayer> {

    /**
     * Constructs a new {@code ImagerySessionExporter}.
     * @param layer imagery layer to export
     */
    public ImagerySessionExporter(ImageryLayer layer) { // NO_UCD (unused code)
        super(layer);
    }

    /**
     * Constructs a new {@code ImagerySessionExporter}.
     * @param layer TMS layer to export
     */
    public ImagerySessionExporter(TMSLayer layer) { // NO_UCD (unused code)
        super(layer);
    }

    /**
     * Constructs a new {@code ImagerySessionExporter}.
     * @param layer WMS layer to export
     */
    public ImagerySessionExporter(WMSLayer layer) { // NO_UCD (unused code)
        super(layer);
    }

    /**
     * Constructs a new {@code ImagerySessionExporter}.
     * @param layer WMTS layer to export
     */
    public ImagerySessionExporter(WMTSLayer layer) { // NO_UCD (unused code)
        super(layer);
    }

    @Override
    public Component getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
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
    public Element export(ExportSupport support) throws IOException {
        Element layerElem = support.createElement("layer");
        layerElem.setAttribute("type", "imagery");
        layerElem.setAttribute("version", "0.1");
        ImageryPreferenceEntry e = new ImageryPreferenceEntry(layer.getInfo());
        Map<String, String> data = new LinkedHashMap<>(Preferences.serializeStruct(e, ImageryPreferenceEntry.class));
        Utils.instanceOfThen(layer, AbstractTileSourceLayer.class, tsLayer -> {
            data.putAll(tsLayer.getDisplaySettings().toPropertiesMap());
            if (!tsLayer.getDisplaySettings().isAutoZoom()) {
                data.put("zoom-level", Integer.toString(tsLayer.getZoomLevel()));
            }
        });
        addAttributes(layerElem, data, support);
        Utils.instanceOfThen(layer, AbstractTileSourceLayer.class, tsLayer -> {
            OffsetBookmark offset = tsLayer.getDisplaySettings().getOffsetBookmark();
            if (offset != null) {
                Map<String, String> offsetProps = offset.toPropertiesMap();
                Element offsetEl = support.createElement("offset");
                layerElem.appendChild(offsetEl);
                addAttributes(offsetEl, offsetProps, support);
            }
        });
        ImageryFilterSettings filters = layer.getFilterSettings();
        if (filters != null) {
            Map<String, String> filterProps = new HashMap<>();
            filters.getProcessors().stream()
                    .flatMap(Utils.castToStream(SessionAwareReadApply.class))
                    .forEach(proc -> filterProps.putAll(proc.toPropertiesMap()));
            if (!filterProps.isEmpty()) {
                Element filterEl = support.createElement("filters");
                layerElem.appendChild(filterEl);
                addAttributes(filterEl, filterProps, support);
            }
        }
        return layerElem;
    }

    private static void addAttributes(Element element, Map<String, String> props, ExportSupport support) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            Element attrElem = support.createElement(entry.getKey());
            element.appendChild(attrElem);
            attrElem.appendChild(support.createTextNode(entry.getValue()));
        }
    }
}
