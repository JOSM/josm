// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.w3c.dom.Element;

/**
 * Session exporter for {@link GpxLayer}.
 * @since 5501
 */
public class GpxTracksSessionExporter extends GenericSessionExporter<GpxLayer> {

    private Instant metaTime;
    private JCheckBox chkMarkers;
    private boolean hasMarkerLayer;

    /**
     * Constructs a new {@code GpxTracksSessionExporter}.
     * @param layer GPX layer to export
     */
    public GpxTracksSessionExporter(GpxLayer layer) { // NO_UCD (test only)
        this(layer, "tracks");
    }

    protected GpxTracksSessionExporter(GpxLayer layer, String type) {
        super(layer, type, "0.1", "gpx");
        if (layer.data == null) {
            throw new IllegalArgumentException("GPX layer without data: " + layer);
        }

        hasMarkerLayer = layer.getLinkedMarkerLayer() != null
                && layer.getLinkedMarkerLayer().data != null
                && !layer.getLinkedMarkerLayer().data.isEmpty();
    }

    @Override
    public JPanel getExportPanel() {
        JPanel p = super.getExportPanel();
        if (hasMarkerLayer) {
            chkMarkers = new JCheckBox();
            chkMarkers.setText(tr("include marker layer \"{0}\"", layer.getLinkedMarkerLayer().getName()));
            chkMarkers.setSelected(true);
            p.add(chkMarkers, GBC.eol().insets(12, 0, 0, 5));
        }
        return p;
    }

    @Override
    public Element export(ExportSupport support) throws IOException {
        Element el = super.export(support);
        if (hasMarkerLayer && (chkMarkers == null || chkMarkers.isSelected())) {
            Element markerEl = support.createElement("markerLayer");
            markerEl.setAttribute("index", Integer.toString(support.getLayerIndexOf(layer.getLinkedMarkerLayer())));
            markerEl.setAttribute("name", layer.getLinkedMarkerLayer().getName());
            markerEl.setAttribute("visible", Boolean.toString(layer.getLinkedMarkerLayer().isVisible()));
            if (layer.getLinkedMarkerLayer().getOpacity() != 1) {
                markerEl.setAttribute("opacity", Double.toString(layer.getLinkedMarkerLayer().getOpacity()));
            }
            el.appendChild(markerEl);
        }
        return el;
    }

    @Override
    @SuppressWarnings("resource")
    protected void addDataFile(OutputStream out) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        GpxWriter w = new GpxWriter(new PrintWriter(writer));
        if (metaTime != null) {
            w.setMetaTime(metaTime);
        }
        w.write(layer.data);
        w.flush();
    }

    protected void setMetaTime(Instant metaTime) {
        this.metaTime = metaTime;
    }
}
