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
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Read content from OSM server for a given URL
 * @since 1146
 */
public class OsmServerLocationReader extends OsmServerReader {

    // CHECKSTYLE.OFF: MethodParamPad
    // CHECKSTYLE.OFF: SingleSpaceSeparator

    /**
     * Patterns for OSM data download URLs.
     * @since 12679
     */
    public enum OsmUrlPattern {
        OSM_API_URL           ("https?://.*/api/0.6/(map|nodes?|ways?|relations?|\\*).*"),
        OVERPASS_API_URL      ("https?://.*/interpreter\\?data=.*"),
        OVERPASS_API_XAPI_URL ("https?://.*/xapi(\\?.*\\[@meta\\]|_meta\\?).*"),
        EXTERNAL_OSM_FILE     ("https?://.*/.*\\.osm");

        private final String urlPattern;

        OsmUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }

        /**
         * Returns the URL pattern.
         * @return the URL pattern
         */
        public String pattern() {
            return urlPattern;
        }
    }

    /**
     * Patterns for GPX download URLs.
     * @since 12679
     */
    public enum GpxUrlPattern {
        TRACE_ID     ("https?://.*(osm|openstreetmap).org/trace/\\p{Digit}+/data"),
        USER_TRACE_ID("https?://.*(osm|openstreetmap).org/user/[^/]+/traces/(\\p{Digit}+)"),
        EDIT_TRACE_ID("https?://.*(osm|openstreetmap).org/edit/?\\?gpx=(\\p{Digit}+)(#.*)?"),

        TRACKPOINTS_BBOX("https?://.*/api/0.6/trackpoints\\?bbox=.*,.*,.*,.*"),
        TASKING_MANAGER("https?://.*/api/v\\p{Digit}+/project/\\p{Digit}+/tasks_as_gpx?.*"),

        EXTERNAL_GPX_SCRIPT("https?://.*exportgpx.*"),
        EXTERNAL_GPX_FILE  ("https?://.*/(.*\\.gpx)");

        private final String urlPattern;

        GpxUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }

        /**
         * Returns the URL pattern.
         * @return the URL pattern
         */
        public String pattern() {
            return urlPattern;
        }
    }

    /**
     * Patterns for Note download URLs.
     * @since 12679
     */
    public enum NoteUrlPattern {
        /** URL of OSM API Notes endpoint */
        API_URL  ("https?://.*/api/0.6/notes.*"),
        /** URL of OSM API Notes compressed dump file */
        DUMP_FILE("https?://.*/(.*\\.osn(\\.(gz|xz|bz2?|zip))?)");

        private final String urlPattern;

        NoteUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
        }

        /**
         * Returns the URL pattern.
         * @return the URL pattern
         */
        public String pattern() {
            return urlPattern;
        }
    }

    // CHECKSTYLE.ON: SingleSpaceSeparator
    // CHECKSTYLE.ON: MethodParamPad

    protected final String url;

    /**
     * Constructs a new {@code OsmServerLocationReader}.
     * @param url The URL to fetch
     */
    public OsmServerLocationReader(String url) {
        this.url = url;
    }

    protected abstract static class Parser<T> {
        protected final ProgressMonitor progressMonitor;
        protected final Compression compression;
        protected InputStream in;

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
            InputStream uncompressedInputStream = compression.getUncompressedInputStream(in);
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
            result.fromServer = isGpxFromServer(url);
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

    /**
     * Determines if the given URL denotes an OSM gpx-related API call.
     * @param url The url to check
     * @return true if the url matches "Trace ID" API call or "Trackpoints bbox" API call, false otherwise
     * @see GpxData#fromServer
     * @since 12679
     */
    public static final boolean isGpxFromServer(String url) {
        return url != null && (url.matches(GpxUrlPattern.TRACE_ID.pattern()) || url.matches(GpxUrlPattern.TRACKPOINTS_BBOX.pattern()));
    }
}
