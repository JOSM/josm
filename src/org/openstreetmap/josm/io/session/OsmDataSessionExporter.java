// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.data.osm.DataSet;
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
        exportData(layer.data, out);
    }

    /**
     * Exports OSM data to the given output stream.
     * @param data data set
     * @param out output stream (must be closed by caller; note: if caller has access, caller should use
     *            {@link org.apache.commons.io.output.CloseShieldOutputStream} when calling this method to
     *            avoid potential future issues)
     */
    @SuppressWarnings("squid:S2095") // All the closeables in this method will close the input OutputStream.
    public static void exportData(DataSet data, OutputStream out) {
        // This writer will close out when it is closed
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        // The PrintWriter will close the writer when it is closed, and the OsmWriter will close the PrintWriter when it is closed.
        OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, data.getVersion());
        data.getReadLock().lock();
        try {
            w.write(data);
            w.flush();
        } finally {
            data.getReadLock().unlock();
        }
    }
}
