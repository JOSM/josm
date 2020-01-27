// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xml.sax.SAXException;

/**
 * GeoJson server reader.
 * @author Omar Vega Ramos &lt;ovruni@riseup.net&gt;
 * @since 15424
 */
public class GeoJSONServerReader extends OsmServerLocationReader {

    /**
     * Constructs a new {@code GeoJSONServerReader}.
     * @param url geojson URL
     */
    public GeoJSONServerReader(String url) {
        super(url);
    }

    protected class GeoJsonParser extends Parser<DataSet> {
        protected GeoJsonParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public DataSet parse() throws OsmTransferException, IllegalDataException, IOException, SAXException {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            InputStream uncompressedInputStream = compression.getUncompressedInputStream(in); // NOPMD
            ProgressMonitor subTaskMonitor = progressMonitor.createSubTaskMonitor(1, false);
            return GeoJSONReader.parseDataSet(uncompressedInputStream, subTaskMonitor);
        }
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return parseOsm(progressMonitor, Compression.NONE);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        return doParse(new GeoJsonParser(progressMonitor, compression), progressMonitor);
    }
}
