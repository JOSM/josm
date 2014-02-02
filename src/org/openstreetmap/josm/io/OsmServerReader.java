// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

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
    private OsmApi api = OsmApi.getOsmApi();
    private boolean doAuthenticate = false;
    protected boolean gpxParsedProperly;

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * Relative URL's are directed to API base URL.
     * @param urlStr The url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @return A reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException thrown if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException  {
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
     * @throws OsmTransferException thrown if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException  {
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
     * @throws OsmTransferException thrown if data transfer errors occur
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
     * @throws OsmTransferException thrown if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, reason, false);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @param uncompressAccordingToContentDisposition Whether to inspect the HTTP header {@code Content-Disposition}
     *                                                for {@code filename} and uncompress a gzip/bzip2 stream.
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException thrown if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason, boolean uncompressAccordingToContentDisposition) throws OsmTransferException {
        try {
            URL url = null;
            try {
                url = new URL(urlStr.replace(" ", "%20"));
            } catch(MalformedURLException e) {
                throw new OsmTransferException(e);
            }
            try {
                // fix #7640, see http://www.tikalk.com/java/forums/httpurlconnection-disable-keep-alive
                activeConnection = Utils.openHttpConnection(url, false);
            } catch(Exception e) {
                throw new OsmTransferException(tr("Failed to open connection to API {0}.", url.toExternalForm()), e);
            }
            if (cancel) {
                activeConnection.disconnect();
                return null;
            }

            if (doAuthenticate) {
                addAuth(activeConnection);
            }
            if (cancel)
                throw new OsmTransferCanceledException();
            if (Main.pref.getBoolean("osm-server.use-compression", true)) {
                activeConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            activeConnection.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);

            try {
                if (reason != null && !reason.isEmpty()) {
                    Main.info("GET " + url + " (" + reason + ")");
                } else {
                    Main.info("GET " + url);
                }
                activeConnection.connect();
            } catch (Exception e) {
                Main.error(e);
                OsmTransferException ote = new OsmTransferException(tr("Could not connect to the OSM server. Please check your internet connection."), e);
                ote.setUrl(url.toString());
                throw ote;
            }
            try {
                Main.debug(activeConnection.getHeaderFields().toString());
                if (activeConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                    throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED,null,null);

                if (activeConnection.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH)
                    throw new OsmTransferCanceledException();

                String encoding = activeConnection.getContentEncoding();
                if (activeConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String errorHeader = activeConnection.getHeaderField("Error");
                    StringBuilder errorBody = new StringBuilder();
                    try {
                        InputStream i = fixEncoding(activeConnection.getErrorStream(), encoding);
                        if (i != null) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(i));
                            String s;
                            while((s = in.readLine()) != null) {
                                errorBody.append(s);
                                errorBody.append("\n");
                            }
                        }
                    }
                    catch(Exception e) {
                        errorBody.append(tr("Reading error text failed."));
                    }

                    throw new OsmApiException(activeConnection.getResponseCode(), errorHeader, errorBody.toString(), url.toString());
                }

                InputStream in = new ProgressInputStream(activeConnection, progressMonitor);
                if (uncompressAccordingToContentDisposition) {
                    in = uncompressAccordingToContentDisposition(in, activeConnection.getHeaderFields());
                }
                return fixEncoding(in, encoding);
            } catch (OsmTransferException e) {
                throw e;
            } catch (Exception e) {
                throw new OsmTransferException(e);
            }
        } finally {
            progressMonitor.invalidate();
        }
    }

    private InputStream fixEncoding(InputStream stream, String encoding) throws IOException {
        if ("gzip".equalsIgnoreCase(encoding)) {
            stream = new GZIPInputStream(stream);
        } else if ("deflate".equalsIgnoreCase(encoding)) {
            stream = new InflaterInputStream(stream, new Inflater(true));
        }
        return stream;
    }

    private InputStream uncompressAccordingToContentDisposition(InputStream stream, Map<String, List<String>> headerFields) throws IOException {
        if (headerFields.get("Content-Disposition").toString().contains(".gz\"")) {
            return Compression.GZIP.getUncompressedInputStream(stream);
        } else if (headerFields.get("Content-Disposition").toString().contains(".bz2\"")) {
            return Compression.BZIP2.getUncompressedInputStream(stream);
        } else {
            return stream;
        }
    }

    /**
     * Download OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public abstract DataSet parseOsm(final ProgressMonitor progressMonitor) throws OsmTransferException;

    /**
     * Download OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChange(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download BZip2-compressed OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChangeBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download GZip-compressed OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChangeGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @param progressMonitor The progress monitor
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     */
    public GpxData parseRawGps(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Retrieve BZip2-compressed GPX files from somewhere.
     * @param progressMonitor The progress monitor
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     * @since 6244
     */
    public GpxData parseRawGpsBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download BZip2-compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download GZip-compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
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
}
