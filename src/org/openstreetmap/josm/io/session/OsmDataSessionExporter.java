// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.tools.JosmRuntimeException;

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
        exportData(layer.data, out);
    }

    /**
     * Exports OSM data to the given output stream.
     * @param data data set
     * @param out output stream
     * @since 15386
     */
    public static void exportData(DataSet data, OutputStream out) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        data.getReadLock().lock();
        try (OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, data.getVersion())) {
            w.write(data);
            w.flush();
        } catch (IOException e) {
            // Catch needed since XmlWriter (parent of OsmWriter) has IOException in the method signature.
            // It doesn't actually throw though. In other words, we should never hit this.
            throw new UncheckedIOException(e);
        } finally {
            data.getReadLock().unlock();
        }
    }
}
