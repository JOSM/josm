// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.NoSuchElementException;
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
            return "interpreter?data=" + Utils.encodeUrl(expandedOverpassQuery);
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
            return super.getInputStreamRaw(urlStr, progressMonitor, reason, uncompressAccordingToContentDisposition);
        } catch (OsmApiException ex) {
            final String errorIndicator = "Error</strong>: ";
            if (ex.getMessage() != null && ex.getMessage().contains(errorIndicator)) {
                final String errorPlusRest = ex.getMessage().split(errorIndicator)[1];
                if (errorPlusRest != null) {
                    final String error = errorPlusRest.split("</")[0];
                    ex.setErrorHeader(error);
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
        return new OverpassOsmReader().doParseDataSet(source, progressMonitor);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {

        DataSet ds = super.parseOsm(progressMonitor);

        // add bounds if necessary (note that Overpass API does not return bounds in the response XML)
        if (ds != null && ds.dataSources.isEmpty() && overpassQuery.contains("{{bbox}}")) {
            if (crosses180th) {
                Bounds bounds = new Bounds(lat1, lon1, lat2, 180.0);
                DataSource src = new DataSource(bounds, getBaseUrl());
                ds.dataSources.add(src);

                bounds = new Bounds(lat1, -180.0, lat2, lon2);
                src = new DataSource(bounds, getBaseUrl());
                ds.dataSources.add(src);
            } else {
                Bounds bounds = new Bounds(lat1, lon1, lat2, lon2);
                DataSource src = new DataSource(bounds, getBaseUrl());
                ds.dataSources.add(src);
            }
        }

        return ds;
    }
}
