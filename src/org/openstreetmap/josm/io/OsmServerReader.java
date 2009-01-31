// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
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
    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param pleaseWaitDlg
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     */
    protected InputStream getInputStream(String urlStr, PleaseWaitDialog pleaseWaitDlg) throws IOException {
        String version = Main.pref.get("osm-server.version", "0.5");
        urlStr = Main.pref.get("osm-server.url")+"/"+version+"/" + urlStr;
        return getInputStreamRaw(urlStr, pleaseWaitDlg);
    }

    protected InputStream getInputStreamRaw(String urlStr, PleaseWaitDialog pleaseWaitDlg) throws IOException {

//        System.out.println("download: "+urlStr);
        initAuthentication();
        URL url = new URL(urlStr);
        activeConnection = (HttpURLConnection)url.openConnection();
        if (cancel) {
            activeConnection.disconnect();
            return null;
        }

        if (Main.pref.getBoolean("osm-server.use-compression", true))
            activeConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

        activeConnection.setConnectTimeout(15000);

        try {
            activeConnection.connect();
        }
        catch (Exception e) {
            throw new IOException(tr("Couldn't connect to the osm server. Please check your internet connection."));
        }

        if (isAuthCancelled() && activeConnection.getResponseCode() == 401)
            return null;
        if (activeConnection.getResponseCode() == 500)
        {
            throw new IOException(tr("Server returned internal error. Try a reduced area or retry after waiting some time."));
        }
//      System.out.println("got return: "+activeConnection.getResponseCode());

        String encoding = activeConnection.getContentEncoding();
        InputStream inputStream = new ProgressInputStream(activeConnection, pleaseWaitDlg);
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
            inputStream = new InflaterInputStream(inputStream, new Inflater(true));
        }
        return inputStream;
    }

    public abstract DataSet parseOsm() throws SAXException, IOException;

}
