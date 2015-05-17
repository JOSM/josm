// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
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

    protected abstract class Parser<T> {
        protected final ProgressMonitor progressMonitor;
        protected final Compression compression;
        protected InputStream in = null;

        public Parser(ProgressMonitor progressMonitor, Compression compression) {
            this.progressMonitor = progressMonitor;
            this.compression = compression;
        }

        public abstract T parse() throws OsmTransferException, IllegalDataException, IOException, SAXException;
    }

    protected final <T> T doParse(Parser<T> parser, final ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting Server...", 10));
        try {
            return parser.parse();
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
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
    public DataSet parseOsm(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmParser(progressMonitor, Compression.NONE), progressMonitor);
    }

    @Override
    public DataSet parseOsmBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmParser(progressMonitor, Compression.BZIP2), progressMonitor);
    }

    @Override
    public DataSet parseOsmGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmParser(progressMonitor, Compression.GZIP), progressMonitor);
    }

    @Override
    public DataSet parseOsmZip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmParser(progressMonitor, Compression.ZIP), progressMonitor);
    }

    @Override
    public DataSet parseOsmChange(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmChangeParser(progressMonitor, Compression.NONE), progressMonitor);
    }

    @Override
    public DataSet parseOsmChangeBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmChangeParser(progressMonitor, Compression.BZIP2), progressMonitor);
    }

    @Override
    public DataSet parseOsmChangeGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new OsmChangeParser(progressMonitor, Compression.GZIP), progressMonitor);
    }

    @Override
    public GpxData parseRawGps(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new GpxParser(progressMonitor, Compression.NONE), progressMonitor);
    }

    @Override
    public GpxData parseRawGpsBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new GpxParser(progressMonitor, Compression.BZIP2), progressMonitor);
    }

    @Override
    public List<Note> parseRawNotes(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new NoteParser(progressMonitor, Compression.NONE), progressMonitor);
    }

    @Override
    public List<Note> parseRawNotesBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return doParse(new NoteParser(progressMonitor, Compression.BZIP2), progressMonitor);
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
            return OsmReader.parseDataSet(compression.getUncompressedInputStream(in), progressMonitor.createSubTaskMonitor(1, false));
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
            result.fromServer = DownloadGpsTask.isFromServer(url);
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
                return new ArrayList<Note>();
            }
            progressMonitor.subTask(tr("Downloading OSM notes..."));
            NoteReader reader = new NoteReader(compression.getUncompressedInputStream(in));
            return reader.parse();
        }
    }
}
