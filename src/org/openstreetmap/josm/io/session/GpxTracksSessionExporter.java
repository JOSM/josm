// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.io.GpxWriter;

/**
 * Session exporter for {@link GpxLayer}.
 * @since 5501
 */
public class GpxTracksSessionExporter extends GenericSessionExporter<GpxLayer> {

    /**
     * Constructs a new {@code GpxTracksSessionExporter}.
     * @param layer GPX layer to export
     */
    public GpxTracksSessionExporter(GpxLayer layer) { // NO_UCD (test only)
        this(layer, "tracks");
    }

    protected GpxTracksSessionExporter(GpxLayer layer, String type) {
        super(layer, type, "0.1", "gpx");
    }

    @Override
    @SuppressWarnings("resource")
    protected void addDataFile(OutputStream out) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        GpxWriter w = new GpxWriter(new PrintWriter(writer));
        w.write(layer.data);
        w.flush();
    }
}
