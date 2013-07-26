// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException thrown if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException  {
        try {
            api.initialize(progressMonitor);
            urlStr = urlStr.startsWith("http") ? urlStr : (getBaseUrl() + urlStr);
            return getInputStreamRaw(urlStr, progressMonitor);
        } finally {
            progressMonitor.invalidate();
        }
    }

    /**
     * Retrun the base URL for relative URL requests
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
                System.out.println("GET " + url);
                activeConnection.connect();
            } catch (Exception e) {
                e.printStackTrace();
                OsmTransferException ote = new OsmTransferException(tr("Could not connect to the OSM server. Please check your internet connection."), e);
                ote.setUrl(url.toString());
                throw ote;
            }
            try {
                if (activeConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                    throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED,null,null);

                if (activeConnection.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH)
                    throw new OsmTransferCanceledException();

                String encoding = activeConnection.getContentEncoding();
                if (activeConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String errorHeader = activeConnection.getHeaderField("Error");
                    StringBuilder errorBody = new StringBuilder();
                    try
                    {
                        InputStream i = FixEncoding(activeConnection.getErrorStream(), encoding);
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

                return FixEncoding(new ProgressInputStream(activeConnection, progressMonitor), encoding);
            } catch(Exception e) {
                if (e instanceof OsmTransferException)
                    throw (OsmTransferException)e;
                else
                    throw new OsmTransferException(e);
            }
        } finally {
            progressMonitor.invalidate();
        }
    }

    private InputStream FixEncoding(InputStream stream, String encoding) throws IOException
    {
        if ("gzip".equalsIgnoreCase(encoding)) {
            stream = new GZIPInputStream(stream);
        }
        else if ("deflate".equalsIgnoreCase(encoding)) {
            stream = new InflaterInputStream(stream, new Inflater(true));
        }
        return stream;
    }

    public abstract DataSet parseOsm(final ProgressMonitor progressMonitor) throws OsmTransferException;

    public DataSet parseOsmChange(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    public DataSet parseOsmChangeBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    public DataSet parseOsmChangeGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    public GpxData parseRawGps(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    public DataSet parseOsmBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

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
