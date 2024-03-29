// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.UrlPatterns.GpxUrlPattern;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Read content from OSM server for a given URL
 * @since 1146
 */
public class OsmServerLocationReader extends OsmServerReader {

    protected final String url;

    /**
     * Constructs a new {@code OsmServerLocationReader}.
     * @param url The URL to fetch
     */
    public OsmServerLocationReader(String url) {
        this.url = url;
    }

    /**
     * Returns the URL to fetch
     * @return the URL to fetch
     * @since 15247
     */
    public final String getUrl() {
        return url;
    }

    protected abstract static class Parser<T> {
        protected final ProgressMonitor progressMonitor;
        protected final Compression compression;
        protected InputStream in;

        protected Parser(ProgressMonitor progressMonitor, Compression compression) {
            this.progressMonitor = progressMonitor;
            this.compression = compression;
        }

        public abstract T parse() throws OsmTransferException, IllegalDataException, IOException, SAXException;
    }

    protected final <T> T doParse(Parser<T> parser, final ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting Server..."), 10);
        try { // NOPMD
            return parser.parse();
        } catch (OsmTransferException e) {
            throw e;
        } catch (IOException | SAXException | IllegalDataException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
            Utils.close(parser.in);
            parser.in = null;
        }
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return parseOsm(progressMonitor, Compression.NONE);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        return doParse(new OsmParser(progressMonitor, compression), progressMonitor);
    }

    @Override
    public DataSet parseOsmChange(ProgressMonitor progressMonitor) throws OsmTransferException {
        return parseOsmChange(progressMonitor, Compression.NONE);
    }

    @Override
    public DataSet parseOsmChange(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        return doParse(new OsmChangeParser(progressMonitor, compression), progressMonitor);
    }

    @Override
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws OsmTransferException {
        return parseRawGps(progressMonitor, Compression.NONE);
    }

    @Override
    public GpxData parseRawGps(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        return doParse(new GpxParser(progressMonitor, compression), progressMonitor);
    }

    @Override
    public List<Note> parseRawNotes(ProgressMonitor progressMonitor) throws OsmTransferException {
        return parseRawNotes(progressMonitor, Compression.NONE);
    }

    @Override
    public List<Note> parseRawNotes(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        return doParse(new NoteParser(progressMonitor, compression), progressMonitor);
    }

    protected class OsmParser extends Parser<DataSet> {
        protected OsmParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            InputStream uncompressedInputStream = compression.getUncompressedInputStream(in); // NOPMD
            ProgressMonitor subTaskMonitor = progressMonitor.createSubTaskMonitor(1, false);
            if ("application/json".equals(contentType)) {
                return OsmJsonReader.parseDataSet(uncompressedInputStream, subTaskMonitor);
            } else {
                return OsmReader.parseDataSet(uncompressedInputStream, subTaskMonitor);
            }
        }
    }

    protected class OsmChangeParser extends Parser<DataSet> {
        protected OsmChangeParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public DataSet parse() throws OsmTransferException, IllegalDataException, IOException {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(9, false));
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            return OsmChangeReader.parseDataSet(compression.getUncompressedInputStream(in), progressMonitor.createSubTaskMonitor(1, false));
        }
    }

    protected class GpxParser extends Parser<GpxData> {
        protected GpxParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public GpxData parse() throws OsmTransferException, IllegalDataException, IOException, SAXException {
            in = getInputStreamRaw(url, progressMonitor.createSubTaskMonitor(1, true), null, true);
            if (in == null)
                return null;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            GpxReader reader = new GpxReader(compression.getUncompressedInputStream(in));
            gpxParsedProperly = reader.parse(false);
            GpxData result = reader.getGpxData();
            result.fromServer = GpxUrlPattern.isGpxFromServer(url);
            return result;
        }
    }

    protected class NoteParser extends Parser<List<Note>> {

        public NoteParser(ProgressMonitor progressMonitor, Compression compression) {
            super(progressMonitor, compression);
        }

        @Override
        public List<Note> parse() throws OsmTransferException, IllegalDataException, IOException, SAXException {
            in = getInputStream(url, progressMonitor.createSubTaskMonitor(1, true));
            if (in == null) {
                return Collections.emptyList();
            }
            progressMonitor.subTask(tr("Downloading OSM notes..."));
            NoteReader reader = new NoteReader(compression.getUncompressedInputStream(in));
            return reader.parse();
        }
    }
}
