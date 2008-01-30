// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.xml.sax.SAXException;

/**
 * Class that uploades all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 *
 * This class implements visitor and will perform the correct upload action on
 * the visited element.
 *
 * @author imi
 */
public class OsmServerWriter extends OsmConnection implements Visitor {

	/**
	 * This list contain all sucessfull processed objects. The caller of
	 * upload* has to check this after the call and update its dataset.
	 *
	 * If a server connection error occours, this may contain fewer entries
	 * than where passed in the list to upload*.
	 */
	public Collection<OsmPrimitive> processed;

	/**
	 * Whether the operation should be aborted as soon as possible.
	 */
	private boolean cancel = false;

	/**
	 * Send the dataset to the server. Ask the user first and does nothing if he
	 * does not want to send the data.
	 */
	private static final int MSECS_PER_SECOND = 1000;
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int MSECS_PER_MINUTE = MSECS_PER_SECOND * SECONDS_PER_MINUTE;

	long uploadStartTime;
	public String timeLeft(int progress, int list_size) {
		long now = System.currentTimeMillis();
		long elapsed = now - uploadStartTime;
		if (elapsed == 0)
			elapsed = 1;
		float uploads_per_ms = (float)progress / elapsed;
		float uploads_left = list_size - progress;
		int ms_left = (int)(uploads_left / uploads_per_ms);
		int minutes_left = ms_left / MSECS_PER_MINUTE;
		int seconds_left = (ms_left / MSECS_PER_SECOND) % SECONDS_PER_MINUTE ;
		String time_left_str = Integer.toString(minutes_left) + ":";
		if (seconds_left < 10)
			time_left_str += "0";
		time_left_str += Integer.toString(seconds_left);
		return time_left_str;
	}	
	public void uploadOsm(Collection<OsmPrimitive> list) throws SAXException {
		processed = new LinkedList<OsmPrimitive>();
		initAuthentication();

		Main.pleaseWaitDlg.progress.setMaximum(list.size());
		Main.pleaseWaitDlg.progress.setValue(0);

		NameVisitor v = new NameVisitor();
		try {
			uploadStartTime = System.currentTimeMillis();
			for (OsmPrimitive osm : list) {
				if (cancel)
					return;
				osm.visit(v);
				int progress = Main.pleaseWaitDlg.progress.getValue();
				String time_left_str = timeLeft(progress, list.size());
				Main.pleaseWaitDlg.currentAction.setText(tr("Upload {0} {1} (id: {2}) {3}% {4}/{5} ({6} left)...",
					tr(v.className), v.name, osm.id, 100.0*progress/list.size(), progress, list.size(), time_left_str));
				osm.visit(this);
				Main.pleaseWaitDlg.progress.setValue(Main.pleaseWaitDlg.progress.getValue()+1);
				Main.pleaseWaitDlg.progress.setValue(progress+1);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new SAXException("An error occoured: "+e.getMessage());
		}
	}

	/**
	 * Upload a single node.
	 */
	public void visit(Node n) {
		if (n.id == 0 && !n.deleted && n.get("created_by") == null) {
			n.put("created_by", "JOSM");
			sendRequest("PUT", "node", n, true);
		} else if (n.deleted) {
			sendRequest("DELETE", "node", n, false);
		} else {
			sendRequest("PUT", "node", n, true);
		}
		processed.add(n);
	}

	/**
	 * Upload a whole way with the complete node id list.
	 */
	public void visit(Way w) {
		if (w.id == 0 && !w.deleted && w.get("created_by") == null) {
			w.put("created_by", "JOSM");
			sendRequest("PUT", "way", w, true);
		} else if (w.deleted) {
			sendRequest("DELETE", "way", w, false);
		} else {
			sendRequest("PUT", "way", w, true);
		}
		processed.add(w);
	}

	/**
	 * Upload an relation with all members.
	 */
	public void visit(Relation e) {
		if (e.id == 0 && !e.deleted && e.get("created_by") == null) {
			e.put("created_by", "JOSM");
			sendRequest("PUT", "relation", e, true);
		} else if (e.deleted) {
			sendRequest("DELETE", "relation", e, false);
		} else {
			sendRequest("PUT", "relation", e, true);
		}
		processed.add(e);
	}
	/**
	 * Read a long from the input stream and return it.
	 */
	private long readId(InputStream inputStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				inputStream));
		String s = in.readLine();
		if (s == null)
			return 0;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Send the request. The objects id will be replaced if it was 0 before
	 * (on add requests).
	 *
	 * @param requestMethod The http method used when talking with the server.
	 * @param urlSuffix The suffix to add at the server url.
	 * @param osm The primitive to encode to the server.
	 * @param addBody <code>true</code>, if the whole primitive body should be added.
	 * 		<code>false</code>, if only the id is encoded.
	 */
	private void sendRequestRetry(String requestMethod, String urlSuffix,
			OsmPrimitive osm, boolean addBody, int retries) {
		try {
			if (cancel)
				return; // assume cancel
			String version = Main.pref.get("osm-server.version", "0.5");
			URL url = new URL(
					Main.pref.get("osm-server.url") +
					"/" + version +
					"/" + urlSuffix + 
					"/" + (osm.id==0 ? "create" : osm.id));
			System.out.print("upload to: "+url+ "..." );
			activeConnection = (HttpURLConnection)url.openConnection();
			activeConnection.setConnectTimeout(15000);
			activeConnection.setRequestMethod(requestMethod);
			if (addBody)
				activeConnection.setDoOutput(true);
			activeConnection.connect();
			System.out.println("connected");
			if (addBody) {
				OutputStream out = activeConnection.getOutputStream();
				OsmWriter.output(out, new OsmWriter.Single(osm, true));
				out.close();
			}

			int retCode = activeConnection.getResponseCode();
			if (retCode == 200 && osm.id == 0)
				osm.id = readId(activeConnection.getInputStream());
			System.out.println("got return: "+retCode+" with id "+osm.id);
			String retMsg = activeConnection.getResponseMessage();
			activeConnection.disconnect();
			if (retCode == 410 && requestMethod.equals("DELETE"))
				return; // everything fine.. was already deleted.
			if (retCode != 200 && retCode != 412) {
				if (retries >= 0) {
					retries--;
					System.out.print("backing off for 10 seconds...");
					Thread.sleep(10000);
					System.out.println("retrying ("+retries+" left)");
					sendRequestRetry(requestMethod, urlSuffix, osm, addBody, retries);
				} else { 
					// Look for a detailed error message from the server
					if (activeConnection.getHeaderField("Error") != null)
						retMsg += "\n" + activeConnection.getHeaderField("Error");

					// Report our error
					ByteArrayOutputStream o = new ByteArrayOutputStream();
					OsmWriter.output(o, new OsmWriter.Single(osm, true));
					System.out.println(new String(o.toByteArray(), "UTF-8").toString());
					throw new RuntimeException(retCode+" "+retMsg);
				}
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(tr("Unknown host")+": "+e.getMessage(), e);
		} catch(SocketTimeoutException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return; // assume cancel
			if (retries-- > 0)
				sendRequestRetry(requestMethod, urlSuffix, osm, addBody, retries);
			else
				throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		} catch (Exception e) {
			if (cancel)
				return; // assume cancel
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
	}
	private void sendRequest(String requestMethod, String urlSuffix,
			OsmPrimitive osm, boolean addBody) {
		sendRequestRetry(requestMethod, urlSuffix, osm, addBody, 10);
	}
}
