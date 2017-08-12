// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Read content from an Overpass server.
 *
 * @since 8744
 */
public class OverpassDownloadReader extends BoundingBoxDownloader {

    private static final String DATA_PREFIX = "?data=";

    static final class OverpassOsmReader extends OsmReader {
        @Override
        protected void parseUnknown(boolean printWarning) throws XMLStreamException {
            if ("remark".equals(parser.getLocalName()) && parser.getEventType() == XMLStreamConstants.START_ELEMENT) {
                final String text = parser.getElementText();
                if (text.contains("runtime error")) {
                    throw new XMLStreamException(text);
                }
            }
            super.parseUnknown(printWarning);
        }
    }

    /**
     * Possible Overpass API output format, with the {@code [out:<directive>]} statement.
     * @since 11916
     */
    public enum OverpassOutpoutFormat {
        /** Default output format: plain OSM XML */
        OSM_XML("xml"),
        /** OSM JSON format (not GeoJson) */
        OSM_JSON("json"),
        /** CSV, see https://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL#Output_Format_.28out.29 */
        CSV("csv"),
        /** Custom, see https://overpass-api.de/output_formats.html#custom */
        CUSTOM("custom"),
        /** Popup, see https://overpass-api.de/output_formats.html#popup */
        POPUP("popup"),
        /** PBF, see https://josm.openstreetmap.de/ticket/14653 */
        PBF("pbf");

        private final String directive;

        OverpassOutpoutFormat(String directive) {
            this.directive = directive;
        }

        /**
         * Returns the directive used in {@code [out:<directive>]} statement.
         * @return the directive used in {@code [out:<directive>]} statement
         */
        public String getDirective() {
            return directive;
        }

        /**
         * Returns the {@code OverpassOutpoutFormat} matching the given directive.
         * @param directive directive used in {@code [out:<directive>]} statement
         * @return {@code OverpassOutpoutFormat} matching the given directive
         * @throws IllegalArgumentException in case of invalid directive
         */
        static OverpassOutpoutFormat from(String directive) {
            for (OverpassOutpoutFormat oof : values()) {
                if (oof.directive.equals(directive)) {
                    return oof;
                }
            }
            throw new IllegalArgumentException(directive);
        }
    }

    static final Pattern OUTPUT_FORMAT_STATEMENT = Pattern.compile(".*\\[out:([a-z]{3,})\\].*", Pattern.DOTALL);

    static final Map<OverpassOutpoutFormat, Class<? extends AbstractReader>> outputFormatReaders = new ConcurrentHashMap<>();

    final String overpassServer;
    final String overpassQuery;

    /**
     * Constructs a new {@code OverpassDownloadReader}.
     *
     * @param downloadArea   The area to download
     * @param overpassServer The Overpass server to use
     * @param overpassQuery  The Overpass query
     */
    public OverpassDownloadReader(Bounds downloadArea, String overpassServer, String overpassQuery) {
        super(downloadArea);
        this.overpassServer = overpassServer;
        this.overpassQuery = overpassQuery.trim();
    }

    /**
     * Registers an OSM reader for the given Overpass output format.
     * @param format Overpass output format
     * @param readerClass OSM reader class
     * @return the previous value associated with {@code format}, or {@code null} if there was no mapping
     */
    public static final Class<? extends AbstractReader> registerOverpassOutpoutFormatReader(
            OverpassOutpoutFormat format, Class<? extends AbstractReader> readerClass) {
        return outputFormatReaders.put(Objects.requireNonNull(format), Objects.requireNonNull(readerClass));
    }

    static {
        registerOverpassOutpoutFormatReader(OverpassOutpoutFormat.OSM_XML, OverpassOsmReader.class);
    }

    @Override
    protected String getBaseUrl() {
        return overpassServer;
    }

    @Override
    protected String getRequestForBbox(double lon1, double lat1, double lon2, double lat2) {
        if (overpassQuery.isEmpty())
            return super.getRequestForBbox(lon1, lat1, lon2, lat2);
        else {
            final String query = this.overpassQuery.replace("{{bbox}}", lat1 + "," + lon1 + "," + lat2 + "," + lon2);
            final String expandedOverpassQuery = expandExtendedQueries(query);
            return "interpreter" + DATA_PREFIX + Utils.encodeUrl(expandedOverpassQuery);
        }
    }

    /**
     * Evaluates some features of overpass turbo extended query syntax.
     * See https://wiki.openstreetmap.org/wiki/Overpass_turbo/Extended_Overpass_Turbo_Queries
     * @param query unexpanded query
     * @return expanded query
     */
    static String expandExtendedQueries(String query) {
        final StringBuffer sb = new StringBuffer();
        final Matcher matcher = Pattern.compile("\\{\\{(geocodeArea):([^}]+)\\}\\}").matcher(query);
        while (matcher.find()) {
            try {
                switch (matcher.group(1)) {
                    case "geocodeArea":
                        matcher.appendReplacement(sb, geocodeArea(matcher.group(2)));
                        break;
                    default:
                        Main.warn("Unsupported syntax: " + matcher.group(1));
                }
            } catch (UncheckedParseException ex) {
                final String msg = tr("Failed to evaluate {0}", matcher.group());
                Main.warn(ex, msg);
                matcher.appendReplacement(sb, "// " + msg + "\n");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String geocodeArea(String area) {
        // Offsets defined in https://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL#By_element_id
        final EnumMap<OsmPrimitiveType, Long> idOffset = new EnumMap<>(OsmPrimitiveType.class);
        idOffset.put(OsmPrimitiveType.NODE, 0L);
        idOffset.put(OsmPrimitiveType.WAY, 2_400_000_000L);
        idOffset.put(OsmPrimitiveType.RELATION, 3_600_000_000L);
        try {
            final PrimitiveId osmId = NameFinder.queryNominatim(area).stream().filter(
                    x -> !OsmPrimitiveType.NODE.equals(x.getOsmId().getType())).iterator().next().getOsmId();
            return String.format("area(%d)", osmId.getUniqueId() + idOffset.get(osmId.getType()));
        } catch (IOException | NoSuchElementException | IndexOutOfBoundsException ex) {
            throw new UncheckedParseException(ex);
        }
    }

    @Override
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason,
                                            boolean uncompressAccordingToContentDisposition) throws OsmTransferException {
        try {
            int index = urlStr.indexOf(DATA_PREFIX);
            // Make an HTTP POST request instead of a simple GET, allows more complex queries
            return super.getInputStreamRaw(urlStr.substring(0, index),
                    progressMonitor, reason, uncompressAccordingToContentDisposition,
                    "POST", Utils.decodeUrl(urlStr.substring(index + DATA_PREFIX.length())).getBytes(StandardCharsets.UTF_8));
        } catch (OsmApiException ex) {
            final String errorIndicator = "Error</strong>: ";
            if (ex.getMessage() != null && ex.getMessage().contains(errorIndicator)) {
                final String errorPlusRest = ex.getMessage().split(errorIndicator)[1];
                if (errorPlusRest != null) {
                    ex.setErrorHeader(errorPlusRest.split("</")[0].replaceAll(".*::request_read_and_idx::", ""));
                }
            }
            throw ex;
        }
    }

    @Override
    protected void adaptRequest(HttpClient request) {
        // see https://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL#timeout
        final Matcher timeoutMatcher = Pattern.compile("\\[timeout:(\\d+)\\]").matcher(overpassQuery);
        final int timeout;
        if (timeoutMatcher.find()) {
            timeout = (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(timeoutMatcher.group(1)));
        } else {
            timeout = (int) TimeUnit.MINUTES.toMillis(3);
        }
        request.setConnectTimeout(timeout);
        request.setReadTimeout(timeout);
    }

    @Override
    protected String getTaskName() {
        return tr("Contacting Server...");
    }

    @Override
    protected DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        AbstractReader reader = null;
        Matcher m = OUTPUT_FORMAT_STATEMENT.matcher(overpassQuery);
        if (m.matches()) {
            Class<? extends AbstractReader> readerClass = outputFormatReaders.get(OverpassOutpoutFormat.from(m.group(1)));
            if (readerClass != null) {
                try {
                    reader = readerClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                    Main.error(e);
                }
            }
        }
        if (reader == null) {
            reader = new OverpassOsmReader();
        }
        return reader.doParseDataSet(source, progressMonitor);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {

        DataSet ds = super.parseOsm(progressMonitor);

        // add bounds if necessary (note that Overpass API does not return bounds in the response XML)
        if (ds != null && ds.getDataSources().isEmpty() && overpassQuery.contains("{{bbox}}")) {
            if (crosses180th) {
                Bounds bounds = new Bounds(lat1, lon1, lat2, 180.0);
                DataSource src = new DataSource(bounds, getBaseUrl());
                ds.addDataSource(src);

                bounds = new Bounds(lat1, -180.0, lat2, lon2);
                src = new DataSource(bounds, getBaseUrl());
                ds.addDataSource(src);
            } else {
                Bounds bounds = new Bounds(lat1, lon1, lat2, lon2);
                DataSource src = new DataSource(bounds, getBaseUrl());
                ds.addDataSource(src);
            }
        }

        return ds;
    }
}
