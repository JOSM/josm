// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;

/**
 * Session exporter for {@link OsmDataLayer}.
 * @since 4685
 */
public class OsmDataSessionExporter extends GenericSessionExporter<OsmDataLayer> {

    /**
     * Constructs a new {@code OsmDataSessionExporter}.
     * @param layer Data layer to export
     */
    public OsmDataSessionExporter(OsmDataLayer layer) { // NO_UCD (test only)
        super(layer, "osm-data", "0.1", "osm");
    }

    @Override
    protected void addDataFile(OutputStream out) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, layer.data.getVersion());
        layer.data.getReadLock().lock();
        try {
            w.write(layer.data);
            w.flush();
        } finally {
            layer.data.getReadLock().unlock();
        }
    }
}
