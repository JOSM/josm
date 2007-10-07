// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitDialog;

/**
 * This DataReader reads directly from the REST API of the osm server.
 * 
 * It supports plain text transfer as well as gzip or deflate encoded transfers;
 * if compressed transfers are unwanted, set property osm-server.use-compression
 * to false.
 *
 * @author imi
 */
abstract class OsmServerReader extends OsmConnection {
	/**
	 * Open a connection to the given url and return a reader on the input stream
	 * from that connection. In case of user cancel, return <code>null</code>.
	 * @param url The exact url to connect to.
	 * @return An reader reading the input stream (servers answer) or <code>null</code>.
	 */
	protected InputStream getInputStream(String urlStr, PleaseWaitDialog pleaseWaitDlg) throws IOException {
		String version = Main.pref.get("osm-server.version", "0.5");
		urlStr = Main.pref.get("osm-server.url")+"/"+version+"/" + urlStr;
		System.out.println("download: "+urlStr);
		initAuthentication();
		URL url = new URL(urlStr);
		activeConnection = (HttpURLConnection)url.openConnection();
		if (cancel) {
			activeConnection.disconnect();
			return null;
		}
		
		if (Boolean.parseBoolean(Main.pref.get("osm-server.use-compression", "true")))
			activeConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

		System.out.println("got return: "+activeConnection.getResponseCode());
		activeConnection.setConnectTimeout(15000);
		if (isAuthCancelled() && activeConnection.getResponseCode() == 401)
			return null;

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
}
