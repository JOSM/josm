// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

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

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param pleaseWaitDlg
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException  {
        try {
            api.initialize(progressMonitor);
            urlStr = api.getBaseUrl() + urlStr;
            return getInputStreamRaw(urlStr, progressMonitor);
        } finally {
            progressMonitor.invalidate();
        }
    }

    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException {
        try {
            URL url = null;
            try {
                url = new URL(urlStr);
            } catch(MalformedURLException e) {
                throw new OsmTransferException(e);
            }
            try {
                activeConnection = (HttpURLConnection)url.openConnection();
            } catch(Exception e) {
                throw new OsmTransferException(tr("Failed to open connection to API {0}", url.toExternalForm()), e);
            }
            if (cancel) {
                activeConnection.disconnect();
                return null;
            }

            try {
                if (doAuthenticate) {
                    addAuth(activeConnection);
                }
            } catch(CharacterCodingException e) {
                System.err.println(tr("Error: failed to add authentication credentials to the connection."));
                throw new OsmTransferException(e);
            }
            if (Main.pref.getBoolean("osm-server.use-compression", true)) {
                activeConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            activeConnection.setConnectTimeout(15000);

            try {
                System.out.println("GET " + url);
                activeConnection.connect();
            } catch (Exception e) {
                throw new OsmTransferException(tr("Couldn't connect to the osm server. Please check your internet connection."), e);
            }
            try {
                if (isAuthCancelled() && activeConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                    throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED,null,null);

                if (activeConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String errorHeader = activeConnection.getHeaderField("Error");
                    InputStream i = null;
                    i = activeConnection.getErrorStream();
                    StringBuilder errorBody = new StringBuilder();
                    if (i != null) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(i));
                        String s;
                        while((s = in.readLine()) != null) {
                            errorBody.append(s);
                            errorBody.append("\n");
                        }
                    }

                    throw new OsmApiException(activeConnection.getResponseCode(), errorHeader, errorBody.toString());
                }

                String encoding = activeConnection.getContentEncoding();
                InputStream inputStream = new ProgressInputStream(activeConnection, progressMonitor);
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                    inputStream = new InflaterInputStream(inputStream, new Inflater(true));
                }
                return inputStream;
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

    public abstract DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException;

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
}
