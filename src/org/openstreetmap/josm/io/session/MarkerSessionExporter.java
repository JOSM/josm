// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.w3c.dom.Element;

/**
 * Session exporter for {@link MarkerLayer}.
 * @since 5684
 */
public class MarkerSessionExporter extends AbstractSessionExporter<MarkerLayer> {

    private Instant metaTime;
    private boolean canExport = true;

    /**
     * Constructs a new {@code MarkerSessionExporter}.
     * @param layer marker layer to export
     */
    public MarkerSessionExporter(MarkerLayer layer) { // NO_UCD (unused code)
        super(layer);
    }

    @Override
    public Collection<Layer> getDependencies() {
        Layer gpxLayer = layer.fromLayer;
        if (gpxLayer != null && MainApplication.getLayerManager().containsLayer(gpxLayer))
            return Collections.singleton(gpxLayer);
        return Collections.emptySet();
    }

    @Override
    public Component getExportPanel() {
        export.setSelected(true); //true even when not shown to the user as the index should be reserved for the corresponding GPX layer
        if (layer.fromLayer != null && layer.fromLayer.getData() != null) {
            canExport = false;
            return null;
        }
        final JPanel p = new JPanel(new GridBagLayout());
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEADING);
        lbl.setToolTipText(layer.getToolTipText());
        lbl.setLabelFor(export);
        p.add(export, GBC.std());
        p.add(lbl, GBC.std());
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        return p;
    }

    @Override
    public boolean requiresZip() {
        return canExport;
    }

    @Override
    public Element export(ExportSupport support) throws IOException {
        if (!canExport) return null;

        Element layerEl = support.createElement("layer");
        layerEl.setAttribute("type", "markers");
        layerEl.setAttribute("version", "0.1");

        Element file = support.createElement("file");
        layerEl.appendChild(file);

        String zipPath = "layers/" + String.format("%02d", support.getLayerIndex()) + "/data.gpx";
        file.appendChild(support.createTextNode(zipPath));
        addDataFile(support.getOutputStreamZip(zipPath));

        return layerEl;
    }

    @SuppressWarnings("resource")
    protected void addDataFile(OutputStream out) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        MarkerWriter w = new MarkerWriter(new PrintWriter(writer));
        if (metaTime != null) {
            w.setMetaTime(metaTime);
        }
        w.write(layer);
        w.flush();
    }

    protected void setMetaTime(Instant metaTime) {
        this.metaTime = metaTime;
    }

    /**
     * Writes GPX file from marker data.
     */
    public static class MarkerWriter extends GpxWriter {

        /**
         * Constructs a new {@code MarkerWriter}.
         * @param out The output writer
         */
        public MarkerWriter(PrintWriter out) {
            super(out);
        }

        /**
         * Writes the given markers data.
         * @param layer The layer data to write
         */
        public void write(MarkerLayer layer) {
            GpxData data = new GpxData();
            layer.data.getLayerPrefs().forEach((k, v) -> {
                if (k != null && k.indexOf("markers.") == 0) {
                    data.getLayerPrefs().put(k, v);
                }
            });
            data.put(GpxData.META_DESC, "exported JOSM marker layer");
            for (Marker m : layer.data) {
                data.waypoints.add(m.convertToWayPoint());
            }
            super.write(data);
        }
    }
}
