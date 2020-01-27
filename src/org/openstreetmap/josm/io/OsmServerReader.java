// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator.RequestorType;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This DataReader reads directly from the REST API of the osm server.
 *
 * It supports plain text transfer as well as gzip or deflate encoded transfers;
 * if compressed transfers are unwanted, set property osm-server.use-compression
 * to false.
 *
 * @author imi
 */
public abstract class OsmServerReader extends OsmConnection {
    private final OsmApi api = OsmApi.getOsmApi();
    private boolean doAuthenticate;
    protected boolean gpxParsedProperly;
    protected String contentType;

    /**
     * Constructs a new {@code OsmServerReader}.
     */
    public OsmServerReader() {
        try {
            doAuthenticate = OsmApi.isUsingOAuth() && CredentialsManager.getInstance().lookupOAuthAccessToken() != null;
        } catch (CredentialsAgentException e) {
            Logging.warn(e);
        }
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * Relative URL's are directed to API base URL.
     * @param urlStr The url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @return A reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException {
        return getInputStream(urlStr, progressMonitor, null);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * Relative URL's are directed to API base URL.
     * @param urlStr The url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return A reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException {
        try {
            api.initialize(progressMonitor);
            String url = urlStr.startsWith("http") ? urlStr : (getBaseUrl() + urlStr);
            return getInputStreamRaw(url, progressMonitor, reason);
        } finally {
            progressMonitor.invalidate();
        }
    }

    /**
     * Return the base URL for relative URL requests
     * @return base url of API
     */
    protected String getBaseUrl() {
        return api.getBaseUrl();
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, null);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, reason, false);
    }

    /**
     * Open a connection to the given url (if HTTP, trough a GET request) and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @param uncompressAccordingToContentDisposition Whether to inspect the HTTP header {@code Content-Disposition}
     *                                                for {@code filename} and uncompress a gzip/bzip2/xz/zip stream.
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason,
            boolean uncompressAccordingToContentDisposition) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, reason, uncompressAccordingToContentDisposition, "GET", null);
    }

    /**
     * Open a connection to the given url (if HTTP, with the specified method) and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @param uncompressAccordingToContentDisposition Whether to inspect the HTTP header {@code Content-Disposition}
     *                                                for {@code filename} and uncompress a gzip/bzip2/xz/zip stream.
     * @param httpMethod HTTP method ("GET", "POST" or "PUT")
     * @param requestBody HTTP request body (for "POST" and "PUT" methods only). Must be null for "GET" method.
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     * @since 12596
     */
    @SuppressWarnings("resource")
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason,
            boolean uncompressAccordingToContentDisposition, String httpMethod, byte[] requestBody) throws OsmTransferException {
        try {
            OnlineResource.JOSM_WEBSITE.checkOfflineAccess(urlStr, Config.getUrls().getJOSMWebsite());
            OnlineResource.OSM_API.checkOfflineAccess(urlStr, OsmApi.getOsmApi().getServerUrl());

            URL url = null;
            try {
                url = new URL(urlStr.replace(" ", "%20"));
            } catch (MalformedURLException e) {
                throw new OsmTransferException(e);
            }

            String protocol = url.getProtocol();
            if ("file".equals(protocol) || "jar".equals(protocol)) {
                try {
                    return Utils.openStream(url);
                } catch (IOException e) {
                    throw new OsmTransferException(e);
                }
            }

            final HttpClient client = HttpClient.create(url, httpMethod)
                    .setFinishOnCloseOutput(false)
                    .setReasonForRequest(reason)
                    .setOutputMessage(tr("Downloading data..."))
                    .setRequestBody(requestBody);
            activeConnection = client;
            adaptRequest(client);
            if (doAuthenticate) {
                addAuth(client);
            }
            if (cancel)
                throw new OsmTransferCanceledException("Operation canceled");

            final HttpClient.Response response;
            try {
                response = client.connect(progressMonitor);
                contentType = response.getContentType();
            } catch (IOException e) {
                Logging.error(e);
                OsmTransferException ote = new OsmTransferException(
                        tr("Could not connect to the OSM server. Please check your internet connection."), e);
                ote.setUrl(url.toString());
                throw ote;
            }
            try {
                if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    CredentialsManager.getInstance().purgeCredentialsCache(RequestorType.SERVER);
                    throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED, null, null);
                }

                if (response.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH)
                    throw new OsmTransferCanceledException("Proxy Authentication Required");

                if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String errorHeader = response.getHeaderField("Error");
                    String errorBody = fetchResponseText(response);
                    throw new OsmApiException(response.getResponseCode(), errorHeader, errorBody, url.toString(), null,
                            contentType);
                }

                response.uncompressAccordingToContentDisposition(uncompressAccordingToContentDisposition);
                return response.getContent();
            } catch (OsmTransferException e) {
                throw e;
            } catch (IOException e) {
                throw new OsmTransferException(e);
            }
        } finally {
            progressMonitor.invalidate();
        }
    }

    private static String fetchResponseText(final HttpClient.Response response) {
        try {
            return response.fetchContent();
        } catch (IOException e) {
            Logging.error(e);
            return tr("Reading error text failed.");
        }
    }

    /**
     * Allows subclasses to modify the request.
     * @param request the prepared request
     * @since 9308
     */
    protected void adaptRequest(HttpClient request) {
    }

    /**
     * Download OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public abstract DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException;

    /**
     * Download compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @param compression compression to use
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     * @since 13352
     */
    public DataSet parseOsm(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        throw new UnsupportedOperationException();
    }

    /**
     * Download OSM Change uncompressed files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChange(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download OSM Change compressed files from somewhere
     * @param progressMonitor The progress monitor
     * @param compression compression to use
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     * @since 13352
     */
    public DataSet parseOsmChange(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @param progressMonitor The progress monitor
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     */
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Retrieve compressed GPX files from somewhere.
     * @param progressMonitor The progress monitor
     * @param compression compression to use
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     * @since 13352
     */
    public GpxData parseRawGps(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if this reader is adding authentication credentials to the read
     * request sent to the server.
     *
     * @return true if this reader is adding authentication credentials to the read
     * request sent to the server
     */
    public boolean isDoAuthenticate() {
        return doAuthenticate;
    }

    /**
     * Sets whether this reader adds authentication credentials to the read
     * request sent to the server.
     *
     * @param doAuthenticate  true if  this reader adds authentication credentials to the read
     * request sent to the server
     */
    public void setDoAuthenticate(boolean doAuthenticate) {
        this.doAuthenticate = doAuthenticate;
    }

    /**
     * Determines if the GPX data has been parsed properly.
     * @return true if the GPX data has been parsed properly, false otherwise
     * @see GpxReader#parse
     */
    public final boolean isGpxParsedProperly() {
        return gpxParsedProperly;
    }

    /**
     * Downloads notes from the API, given API limit parameters
     *
     * @param noteLimit How many notes to download.
     * @param daysClosed Return notes closed this many days in the past. -1 means all notes, ever. 0 means only unresolved notes.
     * @param progressMonitor Progress monitor for user feedback
     * @return List of notes returned by the API
     * @throws OsmTransferException if any errors happen
     */
    public List<Note> parseNotes(int noteLimit, int daysClosed, ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Downloads notes from a given raw URL. The URL is assumed to be complete and no API limits are added
     *
     * @param progressMonitor progress monitor
     * @return A list of notes parsed from the URL
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    public List<Note> parseRawNotes(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download notes from a URL that contains a compressed notes dump file
     * @param progressMonitor progress monitor
     * @param compression compression to use
     * @return A list of notes parsed from the URL
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     * @since 13352
     */
    public List<Note> parseRawNotes(ProgressMonitor progressMonitor, Compression compression) throws OsmTransferException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an attribute from the given DOM node.
     * @param node DOM node
     * @param name attribute name
     * @return attribute value for the given attribute
     * @since 12510
     */
    protected static String getAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    /**
     * DOM document parser.
     * @param <R> resulting type
     * @since 12510
     */
    @FunctionalInterface
    protected interface DomParser<R> {
        /**
         * Parses a given DOM document.
         * @param doc DOM document
         * @return parsed data
         * @throws XmlParsingException if an XML parsing error occurs
         */
        R parse(Document doc) throws XmlParsingException;
    }

    /**
     * Fetches generic data from the DOM document resulting an API call.
     * @param api the OSM API call
     * @param subtask the subtask translated message
     * @param parser the parser converting the DOM document (OSM API result)
     * @param <T> data type
     * @param monitor The progress monitor
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return The converted data
     * @throws OsmTransferException if something goes wrong
     * @since 12510
     */
    public <T> T fetchData(String api, String subtask, DomParser<T> parser, ProgressMonitor monitor, String reason)
            throws OsmTransferException {
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(subtask);
            try (InputStream in = getInputStream(api, monitor.createSubTaskMonitor(1, true), reason)) {
                return parser.parse(XmlUtils.parseSafeDOM(in));
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }
}
