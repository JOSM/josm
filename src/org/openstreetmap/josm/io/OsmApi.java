// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Capabilities.CapabilitiesParser;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class that encapsulates the communications with the <a href="http://wiki.openstreetmap.org/wiki/API_v0.6">OSM API</a>.<br><br>
 *
 * All interaction with the server-side OSM API should go through this class.<br><br>
 *
 * It is conceivable to extract this into an interface later and create various
 * classes implementing the interface, to be able to talk to various kinds of servers.
 *
 */
public class OsmApi extends OsmConnection {

    /**
     * Maximum number of retries to send a request in case of HTTP 500 errors or timeouts
     */
    public static final int DEFAULT_MAX_NUM_RETRIES = 5;

    /**
     * Maximum number of concurrent download threads, imposed by
     * <a href="http://wiki.openstreetmap.org/wiki/API_usage_policy#Technical_Usage_Requirements">
     * OSM API usage policy.</a>
     * @since 5386
     */
    public static final int MAX_DOWNLOAD_THREADS = 2;

    /**
     * Default URL of the standard OSM API.
     * @since 5422
     */
    public static final String DEFAULT_API_URL = "https://api.openstreetmap.org/api";

    // The collection of instantiated OSM APIs
    private static Map<String, OsmApi> instances = new HashMap<>();

    private URL url = null;

    /**
     * Replies the {@link OsmApi} for a given server URL
     *
     * @param serverUrl  the server URL
     * @return the OsmApi
     * @throws IllegalArgumentException if serverUrl is null
     *
     */
    public static OsmApi getOsmApi(String serverUrl) {
        OsmApi api = instances.get(serverUrl);
        if (api == null) {
            api = new OsmApi(serverUrl);
            instances.put(serverUrl,api);
        }
        return api;
    }

    private static String getServerUrlFromPref() {
        return Main.pref.get("osm-server.url", DEFAULT_API_URL);
    }

    /**
     * Replies the {@link OsmApi} for the URL given by the preference <code>osm-server.url</code>
     *
     * @return the OsmApi
     */
    public static OsmApi getOsmApi() {
        return getOsmApi(getServerUrlFromPref());
    }

    /** Server URL */
    private String serverUrl;

    /** Object describing current changeset */
    private Changeset changeset;

    /** API version used for server communications */
    private String version = null;

    /** API capabilities */
    private Capabilities capabilities = null;

    /** true if successfully initialized */
    private boolean initialized = false;

    /**
     * Constructs a new {@code OsmApi} for a specific server URL.
     *
     * @param serverUrl the server URL. Must not be null
     * @throws IllegalArgumentException if serverUrl is null
     */
    protected OsmApi(String serverUrl)  {
        CheckParameterUtil.ensureParameterNotNull(serverUrl, "serverUrl");
        this.serverUrl = serverUrl;
    }

    /**
     * Replies the OSM protocol version we use to talk to the server.
     * @return protocol version, or null if not yet negotiated.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Replies the host name of the server URL.
     * @return the host name of the server URL, or null if the server URL is malformed.
     */
    public String getHost() {
        String host = null;
        try {
            host = (new URL(serverUrl)).getHost();
        } catch (MalformedURLException e) {
            Main.warn(e);
        }
        return host;
    }

    private class CapabilitiesCache extends CacheCustomContent<OsmTransferException> {

        private static final String CAPABILITIES = "capabilities";

        private ProgressMonitor monitor;
        private boolean fastFail;

        public CapabilitiesCache(ProgressMonitor monitor, boolean fastFail) {
            super(CAPABILITIES + getBaseUrl().hashCode(), CacheCustomContent.INTERVAL_WEEKLY);
            this.monitor = monitor;
            this.fastFail = fastFail;
        }

        @Override
        protected void checkOfflineAccess() {
            OnlineResource.OSM_API.checkOfflineAccess(getBaseUrl(getServerUrlFromPref(), "0.6")+CAPABILITIES, getServerUrlFromPref());
        }

        @Override
        protected byte[] updateData() throws OsmTransferException {
            return sendRequest("GET", CAPABILITIES, null, monitor, false, fastFail).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Initializes this component by negotiating a protocol version with the server.
     *
     * @param monitor the progress monitor
     * @throws OsmTransferCanceledException If the initialisation has been cancelled by user.
     * @throws OsmApiInitializationException If any other exception occurs. Use getCause() to get the original exception.
     */
    public void initialize(ProgressMonitor monitor) throws OsmTransferCanceledException, OsmApiInitializationException {
        initialize(monitor, false);
    }

    /**
     * Initializes this component by negotiating a protocol version with the server, with the ability to control the timeout.
     *
     * @param monitor the progress monitor
     * @param fastFail true to request quick initialisation with a small timeout (more likely to throw exception)
     * @throws OsmTransferCanceledException If the initialisation has been cancelled by user.
     * @throws OsmApiInitializationException If any other exception occurs. Use getCause() to get the original exception.
     */
    public void initialize(ProgressMonitor monitor, boolean fastFail) throws OsmTransferCanceledException, OsmApiInitializationException {
        if (initialized)
            return;
        cancel = false;
        try {
            CapabilitiesCache cache = new CapabilitiesCache(monitor, fastFail);
            try {
                initializeCapabilities(cache.updateIfRequiredString());
            } catch (SAXParseException parseException) {
                // XML parsing may fail if JOSM previously stored a corrupted capabilities document (see #8278)
                // In that case, force update and try again
                initializeCapabilities(cache.updateForceString());
            }
            if (capabilities == null) {
                if (Main.isOffline(OnlineResource.OSM_API)) {
                    Main.warn(tr("{0} not available (offline mode)", tr("OSM API")));
                } else {
                    Main.error(tr("Unable to initialize OSM API."));
                }
                return;
            } else if (!capabilities.supportsVersion("0.6")) {
                Main.error(tr("This version of JOSM is incompatible with the configured server."));
                Main.error(tr("It supports protocol version 0.6, while the server says it supports {0} to {1}.",
                        capabilities.get("version", "minimum"), capabilities.get("version", "maximum")));
                return;
            } else {
                version = "0.6";
                initialized = true;
            }

            /* This is an interim solution for openstreetmap.org not currently
             * transmitting their imagery blacklist in the capabilities call.
             * remove this as soon as openstreetmap.org adds blacklists.
             * If you want to update this list, please ask for update of
             * http://trac.openstreetmap.org/ticket/5024
             * This list should not be maintained by each OSM editor (see #9210) */
            if (this.serverUrl.matches(".*openstreetmap.org/api.*") && capabilities.getImageryBlacklist().isEmpty()) {
                capabilities.put("blacklist", "regex", ".*\\.google\\.com/.*");
                capabilities.put("blacklist", "regex", ".*209\\.85\\.2\\d\\d.*");
                capabilities.put("blacklist", "regex", ".*209\\.85\\.1[3-9]\\d.*");
                capabilities.put("blacklist", "regex", ".*209\\.85\\.12[89].*");
            }

            /* This checks if there are any layers currently displayed that
             * are now on the blacklist, and removes them. This is a rare
             * situation - probably only occurs if the user changes the API URL
             * in the preferences menu. Otherwise they would not have been able
             * to load the layers in the first place because they would have
             * been disabled! */
            if (Main.isDisplayingMapView()) {
                for (Layer l : Main.map.mapView.getLayersOfType(ImageryLayer.class)) {
                    if (((ImageryLayer) l).getInfo().isBlacklisted()) {
                        Main.info(tr("Removed layer {0} because it is not allowed by the configured API.", l.getName()));
                        Main.main.removeLayer(l);
                    }
                }
            }

        } catch (OsmTransferCanceledException e) {
            throw e;
        } catch (OsmTransferException e) {
            initialized = false;
            Main.addNetworkError(url, Utils.getRootCause(e));
            throw new OsmApiInitializationException(e);
        } catch (Exception e) {
            initialized = false;
            throw new OsmApiInitializationException(e);
        }
    }

    private synchronized void initializeCapabilities(String xml) throws SAXException, IOException, ParserConfigurationException {
        if (xml != null) {
            capabilities = CapabilitiesParser.parse(new InputSource(new StringReader(xml)));
        }
    }

    /**
     * Makes an XML string from an OSM primitive. Uses the OsmWriter class.
     * @param o the OSM primitive
     * @param addBody true to generate the full XML, false to only generate the encapsulating tag
     * @return XML string
     */
    private String toXml(IPrimitive o, boolean addBody) {
        StringWriter swriter = new StringWriter();
        try (OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(new PrintWriter(swriter), true, version)) {
            swriter.getBuffer().setLength(0);
            osmWriter.setWithBody(addBody);
            osmWriter.setChangeset(changeset);
            osmWriter.header();
            o.accept(osmWriter);
            osmWriter.footer();
            osmWriter.flush();
        } catch (IOException e) {
            Main.warn(e);
        }
        return swriter.toString();
    }

    /**
     * Makes an XML string from an OSM primitive. Uses the OsmWriter class.
     * @param s the changeset
     * @return XML string
     */
    private String toXml(Changeset s) {
        StringWriter swriter = new StringWriter();
        try (OsmWriter osmWriter = OsmWriterFactory.createOsmWriter(new PrintWriter(swriter), true, version)) {
            swriter.getBuffer().setLength(0);
            osmWriter.header();
            osmWriter.visit(s);
            osmWriter.footer();
            osmWriter.flush();
        } catch (IOException e) {
            Main.warn(e);
        }
        return swriter.toString();
    }

    private static String getBaseUrl(String serverUrl, String version) {
        StringBuilder rv = new StringBuilder(serverUrl);
        if (version != null) {
            rv.append("/");
            rv.append(version);
        }
        rv.append("/");
        // this works around a ruby (or lighttpd) bug where two consecutive slashes in
        // an URL will cause a "404 not found" response.
        int p;
        while ((p = rv.indexOf("//", rv.indexOf("://")+2)) > -1) {
            rv.delete(p, p + 1);
        }
        return rv.toString();
    }

    /**
     * Returns the base URL for API requests, including the negotiated version number.
     * @return base URL string
     */
    public String getBaseUrl() {
        return getBaseUrl(serverUrl, version);
    }

    /**
     * Creates an OSM primitive on the server. The OsmPrimitive object passed in
     * is modified by giving it the server-assigned id.
     *
     * @param osm the primitive
     * @param monitor the progress monitor
     * @throws OsmTransferException if something goes wrong
     */
    public void createPrimitive(IPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        String ret = "";
        try {
            ensureValidChangeset();
            initialize(monitor);
            ret = sendRequest("PUT", OsmPrimitiveType.from(osm).getAPIName()+"/create", toXml(osm, true),monitor);
            osm.setOsmId(Long.parseLong(ret.trim()), 1);
            osm.setChangesetId(getChangeset().getId());
        } catch(NumberFormatException e){
            throw new OsmTransferException(tr("Unexpected format of ID replied by the server. Got ''{0}''.", ret));
        }
    }

    /**
     * Modifies an OSM primitive on the server.
     *
     * @param osm the primitive. Must not be null.
     * @param monitor the progress monitor
     * @throws OsmTransferException if something goes wrong
     */
    public void modifyPrimitive(IPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        String ret = null;
        try {
            ensureValidChangeset();
            initialize(monitor);
            // normal mode (0.6 and up) returns new object version.
            ret = sendRequest("PUT", OsmPrimitiveType.from(osm).getAPIName()+"/" + osm.getId(), toXml(osm, true), monitor);
            osm.setOsmId(osm.getId(), Integer.parseInt(ret.trim()));
            osm.setChangesetId(getChangeset().getId());
            osm.setVisible(true);
        } catch(NumberFormatException e) {
            throw new OsmTransferException(tr("Unexpected format of new version of modified primitive ''{0}''. Got ''{1}''.",
                    osm.getId(), ret));
        }
    }

    /**
     * Deletes an OSM primitive on the server.
     * @param osm the primitive
     * @param monitor the progress monitor
     * @throws OsmTransferException if something goes wrong
     */
    public void deletePrimitive(OsmPrimitive osm, ProgressMonitor monitor) throws OsmTransferException {
        ensureValidChangeset();
        initialize(monitor);
        // can't use a the individual DELETE method in the 0.6 API. Java doesn't allow
        // submitting a DELETE request with content, the 0.6 API requires it, however. Falling back
        // to diff upload.
        //
        uploadDiff(Collections.singleton(osm), monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
    }

    /**
     * Creates a new changeset based on the keys in <code>changeset</code>. If this
     * method succeeds, changeset.getId() replies the id the server assigned to the new
     * changeset
     *
     * The changeset must not be null, but its key/value-pairs may be empty.
     *
     * @param changeset the changeset toe be created. Must not be null.
     * @param progressMonitor the progress monitor
     * @throws OsmTransferException signifying a non-200 return code, or connection errors
     * @throws IllegalArgumentException if changeset is null
     */
    public void openChangeset(Changeset changeset, ProgressMonitor progressMonitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(changeset, "changeset");
        try {
            progressMonitor.beginTask(tr("Creating changeset..."));
            initialize(progressMonitor);
            String ret = "";
            try {
                ret = sendRequest("PUT", "changeset/create", toXml(changeset),progressMonitor);
                changeset.setId(Integer.parseInt(ret.trim()));
                changeset.setOpen(true);
            } catch(NumberFormatException e){
                throw new OsmTransferException(tr("Unexpected format of ID replied by the server. Got ''{0}''.", ret));
            }
            progressMonitor.setCustomText(tr("Successfully opened changeset {0}",changeset.getId()));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Updates a changeset with the keys in  <code>changesetUpdate</code>. The changeset must not
     * be null and id &gt; 0 must be true.
     *
     * @param changeset the changeset to update. Must not be null.
     * @param monitor the progress monitor. If null, uses the {@link NullProgressMonitor#INSTANCE}.
     *
     * @throws OsmTransferException if something goes wrong.
     * @throws IllegalArgumentException if changeset is null
     * @throws IllegalArgumentException if changeset.getId() &lt;= 0
     *
     */
    public void updateChangeset(Changeset changeset, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(changeset, "changeset");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        if (changeset.getId() <= 0)
            throw new IllegalArgumentException(tr("Changeset ID > 0 expected. Got {0}.", changeset.getId()));
        try {
            monitor.beginTask(tr("Updating changeset..."));
            initialize(monitor);
            monitor.setCustomText(tr("Updating changeset {0}...", changeset.getId()));
            sendRequest(
                    "PUT",
                    "changeset/" + changeset.getId(),
                    toXml(changeset),
                    monitor
            );
        } catch(ChangesetClosedException e) {
            e.setSource(ChangesetClosedException.Source.UPDATE_CHANGESET);
            throw e;
        } catch(OsmApiException e) {
            String errorHeader = e.getErrorHeader();
            if (e.getResponseCode() == HttpURLConnection.HTTP_CONFLICT && ChangesetClosedException.errorHeaderMatchesPattern(errorHeader))
                throw new ChangesetClosedException(errorHeader, ChangesetClosedException.Source.UPDATE_CHANGESET);
            throw e;
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Closes a changeset on the server. Sets changeset.setOpen(false) if this operation succeeds.
     *
     * @param changeset the changeset to be closed. Must not be null. changeset.getId() &gt; 0 required.
     * @param monitor the progress monitor. If null, uses {@link NullProgressMonitor#INSTANCE}
     *
     * @throws OsmTransferException if something goes wrong.
     * @throws IllegalArgumentException if changeset is null
     * @throws IllegalArgumentException if changeset.getId() &lt;= 0
     */
    public void closeChangeset(Changeset changeset, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(changeset, "changeset");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        if (changeset.getId() <= 0)
            throw new IllegalArgumentException(tr("Changeset ID > 0 expected. Got {0}.", changeset.getId()));
        try {
            monitor.beginTask(tr("Closing changeset..."));
            initialize(monitor);
            /* send "\r\n" instead of empty string, so we don't send zero payload - works around bugs
               in proxy software */
            sendRequest("PUT", "changeset" + "/" + changeset.getId() + "/close", "\r\n", monitor);
            changeset.setOpen(false);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Uploads a list of changes in "diff" form to the server.
     *
     * @param list the list of changed OSM Primitives
     * @param  monitor the progress monitor
     * @return list of processed primitives
     * @throws OsmTransferException if something is wrong
     */
    public Collection<OsmPrimitive> uploadDiff(Collection<? extends OsmPrimitive> list, ProgressMonitor monitor)
            throws OsmTransferException {
        try {
            monitor.beginTask("", list.size() * 2);
            if (changeset == null)
                throw new OsmTransferException(tr("No changeset present for diff upload."));

            initialize(monitor);

            // prepare upload request
            //
            OsmChangeBuilder changeBuilder = new OsmChangeBuilder(changeset);
            monitor.subTask(tr("Preparing upload request..."));
            changeBuilder.start();
            changeBuilder.append(list);
            changeBuilder.finish();
            String diffUploadRequest = changeBuilder.getDocument();

            // Upload to the server
            //
            monitor.indeterminateSubTask(
                    trn("Uploading {0} object...", "Uploading {0} objects...", list.size(), list.size()));
            String diffUploadResponse = sendRequest("POST", "changeset/" + changeset.getId() + "/upload", diffUploadRequest,monitor);

            // Process the response from the server
            //
            DiffResultProcessor reader = new DiffResultProcessor(list);
            reader.parse(diffUploadResponse, monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
            return reader.postProcess(
                    getChangeset(),
                    monitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false)
            );
        } catch(OsmTransferException e) {
            throw e;
        } catch(XmlParsingException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    private void sleepAndListen(int retry, ProgressMonitor monitor) throws OsmTransferCanceledException {
        Main.info(tr("Waiting 10 seconds ... "));
        for (int i=0; i < 10; i++) {
            if (monitor != null) {
                monitor.setCustomText(tr("Starting retry {0} of {1} in {2} seconds ...", getMaxRetries() - retry,getMaxRetries(), 10-i));
            }
            if (cancel)
                throw new OsmTransferCanceledException();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Main.warn("InterruptedException in "+getClass().getSimpleName()+" during sleep");
            }
        }
        Main.info(tr("OK - trying again."));
    }

    /**
     * Replies the max. number of retries in case of 5XX errors on the server
     *
     * @return the max number of retries
     */
    protected int getMaxRetries() {
        int ret = Main.pref.getInteger("osm-server.max-num-retries", DEFAULT_MAX_NUM_RETRIES);
        return Math.max(ret,0);
    }

    /**
     * Determines if JOSM is configured to access OSM API via OAuth
     * @return {@code true} if JOSM is configured to access OSM API via OAuth, {@code false} otherwise
     * @since 6349
     */
    public static final boolean isUsingOAuth() {
        return "oauth".equals(Main.pref.get("osm-server.auth-method", "basic"));
    }

    protected final String sendRequest(String requestMethod, String urlSuffix,String requestBody, ProgressMonitor monitor)
            throws OsmTransferException {
        return sendRequest(requestMethod, urlSuffix, requestBody, monitor, true, false);
    }

    /**
     * Generic method for sending requests to the OSM API.
     *
     * This method will automatically re-try any requests that are answered with a 5xx
     * error code, or that resulted in a timeout exception from the TCP layer.
     *
     * @param requestMethod The http method used when talking with the server.
     * @param urlSuffix The suffix to add at the server url, not including the version number,
     *    but including any object ids (e.g. "/way/1234/history").
     * @param requestBody the body of the HTTP request, if any.
     * @param monitor the progress monitor
     * @param doAuthenticate  set to true, if the request sent to the server shall include authentication
     * credentials;
     * @param fastFail true to request a short timeout
     *
     * @return the body of the HTTP response, if and only if the response code was "200 OK".
     * @throws OsmTransferException if the HTTP return code was not 200 (and retries have
     *    been exhausted), or rewrapping a Java exception.
     */
    protected final String sendRequest(String requestMethod, String urlSuffix,String requestBody, ProgressMonitor monitor,
            boolean doAuthenticate, boolean fastFail) throws OsmTransferException {
        StringBuilder responseBody = new StringBuilder();
        int retries = fastFail ? 0 : getMaxRetries();

        while(true) { // the retry loop
            try {
                url = new URL(new URL(getBaseUrl()), urlSuffix);
                Main.info(requestMethod + " " + url + "... ");
                Main.debug(requestBody);
                // fix #5369, see http://www.tikalk.com/java/forums/httpurlconnection-disable-keep-alive
                activeConnection = Utils.openHttpConnection(url, false);
                activeConnection.setConnectTimeout(fastFail ? 1000 : Main.pref.getInteger("socket.timeout.connect",15)*1000);
                if (fastFail) {
                    activeConnection.setReadTimeout(1000);
                }
                activeConnection.setRequestMethod(requestMethod);
                if (doAuthenticate) {
                    addAuth(activeConnection);
                }

                if ("PUT".equals(requestMethod) || "POST".equals(requestMethod) || "DELETE".equals(requestMethod)) {
                    activeConnection.setDoOutput(true);
                    activeConnection.setRequestProperty("Content-type", "text/xml");
                    try (OutputStream out = activeConnection.getOutputStream()) {
                        // It seems that certain bits of the Ruby API are very unhappy upon
                        // receipt of a PUT/POST message without a Content-length header,
                        // even if the request has no payload.
                        // Since Java will not generate a Content-length header unless
                        // we use the output stream, we create an output stream for PUT/POST
                        // even if there is no payload.
                        if (requestBody != null) {
                            try (BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                                bwr.write(requestBody);
                                bwr.flush();
                            }
                        }
                    }
                }

                activeConnection.connect();
                Main.info(activeConnection.getResponseMessage());
                int retCode = activeConnection.getResponseCode();

                if (retCode >= 500) {
                    if (retries-- > 0) {
                        sleepAndListen(retries, monitor);
                        Main.info(tr("Starting retry {0} of {1}.", getMaxRetries() - retries,getMaxRetries()));
                        continue;
                    }
                }

                // populate return fields.
                responseBody.setLength(0);

                // If the API returned an error code like 403 forbidden, getInputStream
                // will fail with an IOException.
                InputStream i = getConnectionStream();
                if (i != null) {
                    // the input stream can be null if both the input and the error stream
                    // are null. Seems to be the case if the OSM server replies a 401
                    // Unauthorized, see #3887.
                    //
                    String s;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(i, StandardCharsets.UTF_8))) {
                        while((s = in.readLine()) != null) {
                            responseBody.append(s);
                            responseBody.append("\n");
                        }
                    }
                }
                String errorHeader = null;
                // Look for a detailed error message from the server
                if (activeConnection.getHeaderField("Error") != null) {
                    errorHeader = activeConnection.getHeaderField("Error");
                    Main.error("Error header: " + errorHeader);
                } else if (retCode != HttpURLConnection.HTTP_OK && responseBody.length()>0) {
                    Main.error("Error body: " + responseBody);
                }
                activeConnection.disconnect();

                if (Main.isDebugEnabled()) {
                    Main.debug("RESPONSE: "+ activeConnection.getHeaderFields());
                }

                errorHeader = errorHeader == null? null : errorHeader.trim();
                String errorBody = responseBody.length() == 0? null : responseBody.toString().trim();
                switch(retCode) {
                case HttpURLConnection.HTTP_OK:
                    return responseBody.toString();
                case HttpURLConnection.HTTP_GONE:
                    throw new OsmApiPrimitiveGoneException(errorHeader, errorBody);
                case HttpURLConnection.HTTP_CONFLICT:
                    if (ChangesetClosedException.errorHeaderMatchesPattern(errorHeader))
                        throw new ChangesetClosedException(errorBody, ChangesetClosedException.Source.UPLOAD_DATA);
                    else
                        throw new OsmApiException(retCode, errorHeader, errorBody);
                case HttpURLConnection.HTTP_FORBIDDEN:
                    OsmApiException e = new OsmApiException(retCode, errorHeader, errorBody);
                    e.setAccessedUrl(activeConnection.getURL().toString());
                    throw e;
                default:
                    throw new OsmApiException(retCode, errorHeader, errorBody);
                }
            } catch (SocketTimeoutException | ConnectException e) {
                if (retries-- > 0) {
                    continue;
                }
                throw new OsmTransferException(e);
            } catch(IOException e) {
                throw new OsmTransferException(e);
            } catch(OsmTransferException e) {
                throw e;
            }
        }
    }

    private InputStream getConnectionStream() {
        try {
            return activeConnection.getInputStream();
        } catch (IOException ioe) {
            Main.warn(ioe);
            return activeConnection.getErrorStream();
        }
    }

    /**
     * Replies the API capabilities.
     *
     * @return the API capabilities, or null, if the API is not initialized yet
     */
    public synchronized Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Ensures that the current changeset can be used for uploading data
     *
     * @throws OsmTransferException if the current changeset can't be used for uploading data
     */
    protected void ensureValidChangeset() throws OsmTransferException {
        if (changeset == null)
            throw new OsmTransferException(tr("Current changeset is null. Cannot upload data."));
        if (changeset.getId() <= 0)
            throw new OsmTransferException(tr("ID of current changeset > 0 required. Current ID is {0}.", changeset.getId()));
    }

    /**
     * Replies the changeset data uploads are currently directed to
     *
     * @return the changeset data uploads are currently directed to
     */
    public Changeset getChangeset() {
        return changeset;
    }

    /**
     * Sets the changesets to which further data uploads are directed. The changeset
     * can be null. If it isn't null it must have been created, i.e. id &gt; 0 is required. Furthermore,
     * it must be open.
     *
     * @param changeset the changeset
     * @throws IllegalArgumentException if changeset.getId() &lt;= 0
     * @throws IllegalArgumentException if !changeset.isOpen()
     */
    public void setChangeset(Changeset changeset) {
        if (changeset == null) {
            this.changeset = null;
            return;
        }
        if (changeset.getId() <= 0)
            throw new IllegalArgumentException(tr("Changeset ID > 0 expected. Got {0}.", changeset.getId()));
        if (!changeset.isOpen())
            throw new IllegalArgumentException(tr("Open changeset expected. Got closed changeset with id {0}.", changeset.getId()));
        this.changeset = changeset;
    }

    private static StringBuilder noteStringBuilder(Note note) {
        return new StringBuilder().append("notes/").append(note.getId());
    }

    /**
     * Create a new note on the server.
     * @param latlon Location of note
     * @param text Comment entered by user to open the note
     * @param monitor Progress monitor
     * @return Note as it exists on the server after creation (ID assigned)
     * @throws OsmTransferException
     */
    public Note createNote(LatLon latlon, String text, ProgressMonitor monitor) throws OsmTransferException {
        initialize(monitor);
        String noteUrl = new StringBuilder()
            .append("notes?lat=")
            .append(latlon.lat())
            .append("&lon=")
            .append(latlon.lon())
            .append("&text=")
            .append(Utils.encodeUrl(text)).toString();

        String response = sendRequest("POST", noteUrl, null, monitor, true, false);
        return parseSingleNote(response);
    }

    /**
     * Add a comment to an existing note.
     * @param note The note to add a comment to
     * @param comment Text of the comment
     * @param monitor Progress monitor
     * @return Note returned by the API after the comment was added
     * @throws OsmTransferException
     */
    public Note addCommentToNote(Note note, String comment, ProgressMonitor monitor) throws OsmTransferException {
        initialize(monitor);
        String noteUrl = noteStringBuilder(note)
            .append("/comment?text=")
            .append(Utils.encodeUrl(comment)).toString();

        String response = sendRequest("POST", noteUrl, null, monitor, true, false);
        return parseSingleNote(response);
    }

    /**
     * Close a note.
     * @param note Note to close. Must currently be open
     * @param closeMessage Optional message supplied by the user when closing the note
     * @param monitor Progress monitor
     * @return Note returned by the API after the close operation
     * @throws OsmTransferException
     */
    public Note closeNote(Note note, String closeMessage, ProgressMonitor monitor) throws OsmTransferException {
        initialize(monitor);
        String encodedMessage = Utils.encodeUrl(closeMessage);
        StringBuilder urlBuilder = noteStringBuilder(note)
            .append("/close");
        if (encodedMessage != null && !encodedMessage.trim().isEmpty()) {
            urlBuilder.append("?text=");
            urlBuilder.append(encodedMessage);
        }

        String response = sendRequest("POST", urlBuilder.toString(), null, monitor, true, false);
        return parseSingleNote(response);
    }

    /**
     * Reopen a closed note
     * @param note Note to reopen. Must currently be closed
     * @param reactivateMessage Optional message supplied by the user when reopening the note
     * @param monitor Progress monitor
     * @return Note returned by the API after the reopen operation
     * @throws OsmTransferException
     */
    public Note reopenNote(Note note, String reactivateMessage, ProgressMonitor monitor) throws OsmTransferException {
        initialize(monitor);
        String encodedMessage = Utils.encodeUrl(reactivateMessage);
        StringBuilder urlBuilder = noteStringBuilder(note)
            .append("/reopen");
        if (encodedMessage != null && !encodedMessage.trim().isEmpty()) {
            urlBuilder.append("?text=");
            urlBuilder.append(encodedMessage);
        }

        String response = sendRequest("POST", urlBuilder.toString(), null, monitor, true, false);
        return parseSingleNote(response);
    }

    /** Method for parsing API responses for operations on individual notes */
    private Note parseSingleNote(String xml) throws OsmTransferException {
        try {
            List<Note> newNotes = new NoteReader(xml).parse();
            if(newNotes.size() == 1) {
                return newNotes.get(0);
            }
            //Shouldn't ever execute. Server will either respond with an error (caught elsewhere) or one note
            throw new OsmTransferException(tr("Note upload failed"));
        } catch (SAXException|IOException e) {
            Main.error(e, true);
            throw new OsmTransferException(tr("Error parsing note response from server"), e);
        }
    }
}
