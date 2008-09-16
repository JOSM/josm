// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.LinkedList;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.xml.sax.SAXException;
import org.openstreetmap.josm.io.XmlWriter.OsmWriterInterface;

/**
 * Class that uploads all changes to the osm server.
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
	 * This list contain all successful processed objects. The caller of
	 * upload* has to check this after the call and update its dataset.
	 *
	 * If a server connection error occurs, this may contain fewer entries
	 * than where passed in the list to upload*.
	 */
	public Collection<OsmPrimitive> processed;

	/**
	 * Whether the operation should be aborted as soon as possible.
	 */
	// use the inherited variable
	// private boolean cancel = false;

	/**
	 * Object describing current changeset
	 */
	private Changeset changeset;

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
		
		boolean useChangesets = Main.pref.get("osm-server.version", "0.5").equals("0.6");

		String comment = null;
		while( useChangesets && comment == null)
		{
			comment = JOptionPane.showInputDialog(Main.parent, tr("Provide a brief comment as to the changes to you are uploading:"),
		                                             tr("Commit comment"), JOptionPane.QUESTION_MESSAGE);
			if( comment == null )
				return;
			/* Don't let people just hit enter */
			if( comment.trim().length() >= 3 )
				break;
			comment = null;
		}
		try {
			if( useChangesets && !startChangeset(10, comment) )
				return;
		}
		catch (OsmTransferException ex) {
			dealWithTransferException (ex);
			return;
		}
		
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
				if( useChangesets ) 
					stopChangeset(10);
		} catch (RuntimeException e) {
			try {
				if( useChangesets ) stopChangeset(10);
			}
			catch (OsmTransferException ex) {
				dealWithTransferException(ex);
			}
			e.printStackTrace();
			throw new SAXException(tr("An error occoured: {0}",e.getMessage()));
		}
		catch (OsmTransferException e) {
			try {
				if( useChangesets ) stopChangeset(10);
			}
			catch (OsmTransferException ex) {
				dealWithTransferException(ex);	
			}
			dealWithTransferException(e);
		}
	}
	
	/* FIXME: This code is terrible, please fix it!!!! */

	/* Ok, this needs some explanation: The problem is that the code for
	 * retrying requests is intertwined with the code that generates the
	 * actual request. This means that for the retry code for the
	 * changeset stuff, it's basically a copy/cut/change slightly
	 * process. What actually needs to happen is that the retrying needs
	 * to be split from the creation of the requests and the retry loop
	 * handled in one place (preferably without recursion). While at you
	 * can fix the issue where hitting cancel doesn't do anything while
	 * retrying. - Mv0 Apr 2008
	 * 
	 * Cancelling has an effect now, maybe it does not always catch on. Florian Heer, Aug 08
	 */
	private boolean startChangeset(int retries, String comment) throws OsmTransferException {
		Main.pleaseWaitDlg.currentAction.setText(tr("Opening changeset..."));
		changeset = new Changeset();
		changeset.put( "created_by", "JOSM" );
		changeset.put( "comment", comment );
		try {
			if (cancel)
				return false; // assume cancel
			String version = Main.pref.get("osm-server.version", "0.6");
			URL url = new URL(
					Main.pref.get("osm-server.url") +
					"/" + version +
					"/" + "changeset" + 
					"/" + "create");
			System.out.print("upload to: "+url+ "..." );
			activeConnection = (HttpURLConnection)url.openConnection();
			activeConnection.setConnectTimeout(15000);
			activeConnection.setRequestMethod("PUT");
			addAuth(activeConnection);
			
			activeConnection.setDoOutput(true);
			OutputStream out = activeConnection.getOutputStream();
			OsmWriter.output(out, changeset);
			out.close();
			
			activeConnection.connect();
			System.out.println("connected");

			int retCode = activeConnection.getResponseCode();
			if (retCode == 200)
				changeset.id = readId(activeConnection.getInputStream());
			System.out.println("got return: "+retCode+" with id "+changeset.id);
			String retMsg = activeConnection.getResponseMessage();
			activeConnection.disconnect();
			if (retCode == 404)
			{
				System.out.println("Server does not support changesets, continuing");
				return true;
			}
			if (retCode != 200 && retCode != 412) {
				if (retries >= 0) {
					retries--;
					System.out.print("backing off for 10 seconds...");
					Thread.sleep(10000);
					System.out.println("retrying ("+retries+" left)");
					return startChangeset(retries, comment);
				} else { 
					// Look for a detailed error message from the server
					if (activeConnection.getHeaderField("Error") != null)
						retMsg += "\n" + activeConnection.getHeaderField("Error");

					// Report our error
					ByteArrayOutputStream o = new ByteArrayOutputStream();
					OsmWriter.output(o, changeset);
					System.out.println(new String(o.toByteArray(), "UTF-8").toString());
					//throw new RuntimeException(retCode+" "+retMsg);
					throw new OsmTransferException (retCode + " " + retMsg);
				}
			}
		} catch (UnknownHostException e) {
			//throw new RuntimeException(tr("Unknown host")+": "+e.getMessage(), e);
			throw new OsmTransferException(tr("Unknown host")+": "+e.getMessage(), e);
		} catch(SocketTimeoutException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return false; // assume cancel
			if (retries-- > 0)
				startChangeset(retries, comment);
			else
				// throw new RuntimeException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
		catch (ConnectException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return false; // assume cancel
			if (retries-- > 0)
				startChangeset(retries, comment);
			else
				// throw new RuntimeException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
		
		catch (Exception e) {
			if (cancel)
				return false; // assume cancel
			if (e instanceof OsmTransferException)
				throw (OsmTransferException)e;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
		return true;
	}

	private void stopChangeset(int retries) throws OsmTransferException {
		Main.pleaseWaitDlg.currentAction.setText(tr("Closing changeset..."));
		try {
			if (cancel)
				return; // assume cancel
			String version = Main.pref.get("osm-server.version", "0.6");
			URL url = new URL(
					Main.pref.get("osm-server.url") +
					"/" + version +
					"/" + "changeset" + 
					"/" + changeset.id +
					"/close" );
			System.out.print("upload to: "+url+ "..." );
			activeConnection = (HttpURLConnection)url.openConnection();
			activeConnection.setConnectTimeout(15000);
			activeConnection.setRequestMethod("PUT");
			addAuth(activeConnection);
			
			activeConnection.setDoOutput(true);
			OutputStream out = activeConnection.getOutputStream();
			OsmWriter.output(out, changeset);
			out.close();
			
			activeConnection.connect();
			System.out.println("connected");

			int retCode = activeConnection.getResponseCode();
			if (retCode == 200)
				changeset.id = readId(activeConnection.getInputStream());
			System.out.println("got return: "+retCode+" with id "+changeset.id);
			String retMsg = activeConnection.getResponseMessage();
			activeConnection.disconnect();
			if (retCode == 404)
			{
				System.out.println("Server does not support changesets, continuing");
				return;
			}
			if (retCode != 200 && retCode != 412) {
				if (retries >= 0) {
					retries--;
					System.out.print("backing off for 10 seconds...");
					Thread.sleep(10000);
					System.out.println("retrying ("+retries+" left)");
					stopChangeset(retries);
				} else { 
					// Look for a detailed error message from the server
					if (activeConnection.getHeaderField("Error") != null)
						retMsg += "\n" + activeConnection.getHeaderField("Error");

					// Report our error
					ByteArrayOutputStream o = new ByteArrayOutputStream();
					OsmWriter.output(o, changeset);
					System.out.println(new String(o.toByteArray(), "UTF-8").toString());
					//throw new RuntimeException(retCode+" "+retMsg);
					throw new OsmTransferException(retCode+" "+retMsg);
				}
			}
		} catch (UnknownHostException e) {
			//throw new RuntimeException(tr("Unknown host")+": "+e.getMessage(), e);
			throw new OsmTransferException(tr("Unknown host")+": "+e.getMessage(), e);
		} catch(SocketTimeoutException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return; // assume cancel
			if (retries-- > 0)
				stopChangeset(retries);
			else
				//throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		} catch(ConnectException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return; // assume cancel
			if (retries-- > 0)
				stopChangeset(retries);
			else
				//throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		} catch (Exception e) {
			if (cancel)
				return; // assume cancel
			if (e instanceof OsmTransferException)
				throw (OsmTransferException)e;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
	}

	/**
	 * Upload a single node.
	 */
	public void visit(Node n) {
		if (n.deleted) {
			sendRequest("DELETE", "node", n, true);
		} else {
			sendRequest("PUT", "node", n, true);
		}
		processed.add(n);
	}

	/**
	 * Upload a whole way with the complete node id list.
	 */
	public void visit(Way w) {
		if (w.deleted) {
			sendRequest("DELETE", "way", w, true);
		} else {
			sendRequest("PUT", "way", w, true);
		}
		processed.add(w);
	}

	/**
	 * Upload an relation with all members.
	 */
	public void visit(Relation e) {
		if (e.deleted) {
			sendRequest("DELETE", "relation", e, true);
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
	 * @param body the body to be sent
	 */
	private void sendRequestRetry(String requestMethod, String urlSuffix,
			OsmPrimitive osm, OsmWriterInterface body, int retries) throws OsmTransferException {
		try {
			if (cancel)
				return; // assume cancel
			String version = Main.pref.get("osm-server.version", "0.5");
			URL url = new URL(
					new URL(Main.pref.get("osm-server.url") +
					"/" + version + "/"),
					urlSuffix + 
					"/" + (osm.id==0 ? "create" : osm.id),
					new MyHttpHandler());
			System.out.print("upload to: "+url+ "..." );
			activeConnection = (HttpURLConnection)url.openConnection();
			activeConnection.setConnectTimeout(15000);
			activeConnection.setRequestMethod(requestMethod);
			addAuth(activeConnection);
			if (body != null) {
				activeConnection.setDoOutput(true);
				OutputStream out = activeConnection.getOutputStream();
				OsmWriter.output(out, body);
				out.close();
			}
			activeConnection.connect();
			System.out.println("connected");

			int retCode = activeConnection.getResponseCode();
			/* When creating new, the returned value is the new id, otherwise it is the new version */
			if (retCode == 200)
			{
				if(osm.id == 0)
				{
					osm.id = readId(activeConnection.getInputStream());
					osm.version = 1;
				}
				else
				{
					int read_version = (int)readId(activeConnection.getInputStream());
					if( read_version > 0 )
						osm.version = read_version;
				}
			}
			else
			{
				System.out.println("got return: "+retCode+" with id "+osm.id);
			}
			activeConnection.disconnect();
			if (retCode == 410 && requestMethod.equals("DELETE"))
				return; // everything fine.. was already deleted.
			else if (retCode != 200)
			{
				if (retries >= 0 && retCode != 412)
				{
					retries--;
					System.out.print("backing off for 10 seconds...");
					Thread.sleep(10000);
					System.out.println("retrying ("+retries+" left)");
					sendRequestRetry(requestMethod, urlSuffix, osm, body, retries);
				} else {
					String retMsg = activeConnection.getResponseMessage();
					// Look for a detailed error message from the server
					if (activeConnection.getHeaderField("Error") != null)
						retMsg += "\n" + activeConnection.getHeaderField("Error");

					// Report our error
					ByteArrayOutputStream o = new ByteArrayOutputStream();
					OsmWriter.output(o, body);
					System.out.println(new String(o.toByteArray(), "UTF-8").toString());
					//throw new RuntimeException(retCode+" "+retMsg);
					throw new OsmTransferException(retCode+" "+retMsg);
				}
			}
		} catch (UnknownHostException e) {
			//throw new RuntimeException(tr("Unknown host")+": "+e.getMessage(), e);
			throw new OsmTransferException(tr("Unknown host")+": "+e.getMessage(), e);
		} catch(SocketTimeoutException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return; // assume cancel
			if (retries-- > 0)
				sendRequestRetry(requestMethod, urlSuffix, osm, body, retries);
			else
				//throw new RuntimeException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		} catch(ConnectException e) {
			System.out.println(" timed out, retries left: " + retries);
			if (cancel)
				return; // assume cancel
			if (retries-- > 0)
				sendRequestRetry(requestMethod, urlSuffix, osm, body, retries);
			else
				//throw new RuntimeException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
				throw new OsmTransferException (e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		} catch (Exception e) {
			if (cancel)
				return; // assume cancel
			if (e instanceof OsmTransferException)
				throw (OsmTransferException)e;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
		}
	}
	
	private void sendRequest(String requestMethod, String urlSuffix,
			OsmPrimitive osm, boolean addBody)  {
		XmlWriter.OsmWriterInterface body = null;
		if (addBody) {
				body = new OsmWriter.Single(osm, true, changeset);
		}
		try {
			sendRequestRetry(requestMethod, urlSuffix, osm, body, 10);
		}
		catch (OsmTransferException e) {
			dealWithTransferException (e);
		}
	}
	
	private void dealWithTransferException (OsmTransferException e) {
		Main.pleaseWaitDlg.currentAction.setText(tr("Transfer aborted due to error (will wait now 5 seconds):") + e.getMessage());
		cancel = true;
		try {
			Thread.sleep(5000);
		}
		catch (InterruptedException ex) {}
	}
}
