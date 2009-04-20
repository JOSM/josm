//License: GPL. See README for details.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.CreateOsmChangeVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class that encapsulates the communications with the OSM API. 
 * 
 * All interaction with the server-side OSM API should go through this class. 
 *
 * It is conceivable to extract this into an interface later and create various
 * classes implementing the interface, to be able to talk to various kinds of servers.
 * 
 */
public class OsmApi extends OsmConnection {

    /**
     * Object describing current changeset
     */
    private Changeset changeset;

    /**
     * API version used for server communications
     */
    private String version = null;

    /**
     * Minimum API version accepted by server, from capabilities response
     */
    private String minVersion = null;
    
    /**
     * Maximum API version accepted by server, from capabilities response
     */
    private String maxVersion = null;
    
    /**
     * Maximum downloadable area from server (degrees squared), from capabilities response
     * FIXME: make download dialog use this, instead of hard-coded default.
     */
    private String maxArea = null;
    
    /**
     * true if successfully initialized
     */
    private boolean initialized = false;

    /**
     * list of server error messages (as transported in the Error: header) and their translations.
     */
    public static final HashMap<String,String> serverErrorTranslations;

    static {
        serverErrorTranslations = new HashMap<String,String>();
        serverErrorTranslations.put("Database offline for maintenance", 
            tr("Database offline for maintenance"));
        serverErrorTranslations.put("You must make your edits public to upload new data",
            tr("You must make your edits public to upload new data"));
        // FIXME there is one additional server error message that goes
        // "You requested too many nodes (limit is #{APP_CONFIG['max_number_of_nodes']}). Either request a smaller area, or use planet.osm"
        // but we would have to switch this mechanism to using regular expressions if we wanted to translate that...
    }
    
    private StringWriter swriter = new StringWriter();
    private OsmWriter osmWriter = new OsmWriter(new PrintWriter(swriter), true, null);

    /**
     * A parser for the "capabilities" response XML
     */
    private class CapabilitiesParser extends DefaultHandler {
        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals("version")) {
                minVersion = atts.getValue("minimum");
                maxVersion = atts.getValue("maximum");
            } else if (qName.equals("area"))
                maxArea = atts.getValue("maximum");
        }
    }

    /**
     * Helper that returns the lower-case type name of an OsmPrimitive
     * @param o the primitive
     * @return "node", "way", "relation", or "changeset"
     */
    public static String which(OsmPrimitive o) {
        if (o instanceof Node) return "node";
        if (o instanceof Way) return "way";
        if (o instanceof Relation) return "relation";
        if (o instanceof Changeset) return "changeset";
        return "";
    }
    
    /** 
     * Returns the OSM protocol version we use to talk to the server.
     * @return protocol version, or null if not yet negotiated.
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Returns true if the negotiated version supports changesets.
     * @return true if the negotiated version supports changesets.
     */
    public boolean hasChangesetSupport() {
        return ((version != null) && (version.compareTo("0.6")>=0));
    }
    
    /**
     * Initializes this component by negotiating a protocol version with the server.
     */
    public void initialize() {
        initAuthentication();
        try {
            initialized = true; // note: has to be before the sendRequest or that will throw!
            String capabilities = sendRequest("GET", "capabilities", null);
            InputSource inputSource = new InputSource(new StringReader(capabilities));
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, new CapabilitiesParser());
            if (maxVersion.compareTo("0.6") >= 0) {
                version = "0.6";
            } else if (minVersion.compareTo("0.5") <= 0) {
                version = "0.5";
            } else {
                System.err.println(tr("This version of JOSM is incompatible with the configured server."));
                System.err.println(tr("It supports protocol versions 0.5 and 0.6, while the server says it supports {0} to {1}.", 
                    minVersion, maxVersion));
                initialized = false;
            }
            System.out.println(tr("Communications with {0} established using protocol version {1}", 
                Main.pref.get("osm-server.url"), 
                version));
            osmWriter.setVersion(version);
        } catch (Exception ex) {
            initialized = false;
            ex.printStackTrace();
        }
    }
    
    /**
     * Makes an XML string from an OSM primitive. Uses the OsmWriter class.
     * @param o the OSM primitive
     * @param addBody true to generate the full XML, false to only generate the encapsulating tag
     * @return XML string
     */
    private String toXml(OsmPrimitive o, boolean addBody) {
        swriter.getBuffer().setLength(0);
        osmWriter.setWithBody(addBody);
        osmWriter.header();
        o.visit(osmWriter);
        osmWriter.footer();
        osmWriter.out.flush();
        return swriter.toString();
    }
    
    /**
     * Helper that makes an int from the first whitespace separated token in a string.
     * @param s the string
     * @return the integer represenation of the first token in the string
     * @throws OsmTransferException if the string is empty or does not represent a number
     */
    public static int parseInt(String s) throws OsmTransferException {
        StringTokenizer t = new StringTokenizer(s);
        try {
            return Integer.parseInt(t.nextToken());
        } catch (Exception x) {
            throw new OsmTransferException("Cannot read numeric value from response");
        }
    }

    /**
     * Helper that makes a long from the first whitespace separated token in a string.
     * @param s the string
     * @return the long represenation of the first token in the string
     * @throws OsmTransferException if the string is empty or does not represent a number
     */
    public static long parseLong(String s) throws OsmTransferException {
        StringTokenizer t = new StringTokenizer(s);
        try {
            return Long.parseLong(t.nextToken());
        } catch (Exception x) {
            throw new OsmTransferException("Cannot read numeric value from response");
        }
    }    
    
    /**
     * Returns the base URL for API requests, including the negotiated version number.
     * @return base URL string
     */
    public String getBaseUrl() {
        StringBuffer rv = new StringBuffer(Main.pref.get("osm-server.url"));
        if (version != null) {
            rv.append("/");
            rv.append(version);
        }
        rv.append("/");
        // this works around a ruby (or lighttpd) bug where two consecutive slashes in 
        // an URL will cause a "404 not found" response.
        int p; while ((p = rv.indexOf("//", 6)) > -1) { rv.delete(p, p + 1); }
        return rv.toString();
    }

    /** 
     * Creates an OSM primitive on the server. The OsmPrimitive object passed in
     * is modified by giving it the server-assigned id.
     * 
     * @param osm the primitive
     * @throws OsmTransferException if something goes wrong
     */
    public void createPrimitive(OsmPrimitive osm) throws OsmTransferException {
        osm.id = parseLong(sendRequest("PUT", which(osm)+"/create", toXml(osm, true)));
        osm.version = 1;
    }
    
    /**
     * Modifies an OSM primitive on the server. For protocols greater than 0.5,
     * the OsmPrimitive object passed in is modified by giving it the server-assigned 
     * version.
     * 
     * @param osm the primitive
     * @throws OsmTransferException if something goes wrong
     */
    public void modifyPrimitive(OsmPrimitive osm) throws OsmTransferException {
        if (version.equals("0.5")) {
            // legacy mode does not return the new object version.
            sendRequest("PUT", which(osm)+"/" + osm.id, toXml(osm, true));
        } else {
            // normal mode (0.6 and up) returns new object version.
            osm.version = parseInt(sendRequest("PUT", which(osm)+"/" + osm.id, toXml(osm, true)));
        }
    }    
    
    /**
     * Deletes an OSM primitive on the server.
     * @param osm the primitive
     * @throws OsmTransferException if something goes wrong
     */
    public void deletePrimitive(OsmPrimitive osm) throws OsmTransferException {
        // legacy mode does not require payload. normal mode (0.6 and up) requires payload for version matching.
        sendRequest("DELETE", which(osm)+"/" + osm.id, version.equals("0.5") ? null : toXml(osm, false));
    }   
    
    /**
     * Creates a new changeset on the server to use for subsequent calls.
     * @param comment the "commit comment" for the new changeset
     * @throws OsmTransferException signifying a non-200 return code, or connection errors
     */
    public void createChangeset(String comment) throws OsmTransferException {
        changeset = new Changeset();
        Main.pleaseWaitDlg.currentAction.setText(tr("Opening changeset..."));
        Properties sysProp = System.getProperties();
        Object ua = sysProp.get("http.agent");
        changeset.put("created_by", (ua == null) ? "JOSM" : ua.toString());
        changeset.put("comment", comment);
        createPrimitive(changeset);
    }

    /**
     * Closes a changeset on the server.
     * 
     * @throws OsmTransferException if something goes wrong.
     */
    public void stopChangeset() throws OsmTransferException {
        Main.pleaseWaitDlg.currentAction.setText(tr("Closing changeset..."));
        sendRequest("PUT", "changeset" + "/" + changeset.id + "/close", null);
        changeset = null;
    }

    /**
     * Uploads a list of changes in "diff" form the the server. 
     * @param list the list of changed OSM Primitives
     * @return list of processed primitives
     * @throws OsmTransferException if something is wrong.
     */
    public Collection<OsmPrimitive> uploadDiff(Collection<OsmPrimitive> list) throws OsmTransferException {
    
        if (changeset == null) {
            throw new OsmTransferException(tr("No changeset present for diff upload"));
        }
        
        CreateOsmChangeVisitor duv = new CreateOsmChangeVisitor(changeset, this);
        
        ArrayList<OsmPrimitive> processed = new ArrayList<OsmPrimitive>();
    
        for (OsmPrimitive osm : list) {
            int progress = Main.pleaseWaitDlg.progress.getValue();
            Main.pleaseWaitDlg.currentAction.setText(tr("Preparing..."));
            if (cancel) throw new OsmTransferCancelledException();
            osm.visit(duv);
            Main.pleaseWaitDlg.progress.setValue(progress+1);
        }
    
        Main.pleaseWaitDlg.currentAction.setText(tr("Uploading..."));
        if (cancel) throw new OsmTransferCancelledException();
    
        String diff = duv.getDocument();
        String diffresult = sendRequest("POST", "changeset/" + changeset.id + "/upload", diff);  
        try {
            DiffResultReader.parseDiffResult(diffresult, list, processed, duv.getNewIdMap(), Main.pleaseWaitDlg);
        } catch (Exception sxe) {
            throw new OsmTransferException(tr("Error processing changeset upload response"), sxe);
        }
        return processed;
    }

    private void sleepAndListen() throws OsmTransferCancelledException {
        // System.out.print("backing off for 10 seconds...");
        for(int i=0; i < 10; i++) {
            if (cancel || isAuthCancelled()) {
                throw new OsmTransferCancelledException();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {}
        }
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
     * 
     * @return the body of the HTTP response, if and only if the response code was "200 OK".
     * @exception OsmTransferException if the HTTP return code was not 200 (and retries have 
     *    been exhausted), or rewrapping a Java exception. 
     */
    private String sendRequest(String requestMethod, String urlSuffix,
            String requestBody) throws OsmTransferException {

        if (!initialized) throw new OsmTransferException(tr("Not initialized"));
        
        StringBuffer responseBody = new StringBuffer();
        StringBuffer statusMessage = new StringBuffer();

        int retries = 5; // configurable? 
        
        while(true) { // the retry loop
            try {
                URL url = new URL(new URL(getBaseUrl()), urlSuffix, new MyHttpHandler());
                System.out.print(requestMethod + " " + url + "... ");
                activeConnection = (HttpURLConnection)url.openConnection();
                activeConnection.setConnectTimeout(15000);
                activeConnection.setRequestMethod(requestMethod);
                addAuth(activeConnection);

                if (requestBody != null) {
                    activeConnection.setDoOutput(true);
                    OutputStream out = activeConnection.getOutputStream();
                    BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    bwr.write(requestBody);
                    bwr.flush();
                    out.close();
                } else {
                    // It seems that certain bits of the Ruby API are very unhappy upon
                    // receipt of a PUT/POST message withtout a Content-length header,
                    // even if the request has no payload.
                    // Since Java will not generate a Content-length header unless
                    // we use the output stream, we create one and write no data to 
                    // it, instead of not creating one. -- fr 6-Apr-09
                    activeConnection.setDoOutput(true);
                    OutputStream out = activeConnection.getOutputStream();
                    out.close();
                }
                activeConnection.connect();
                System.out.println(activeConnection.getResponseMessage());

                int retCode = activeConnection.getResponseCode();
                
                if (retCode >= 500) {
                    if (retries-- > 0) {
                        sleepAndListen();
                        continue;
                    }
                }

                // populate return fields.
                responseBody.setLength(0);
                BufferedReader in = new BufferedReader(new InputStreamReader(activeConnection.getInputStream()));
                String s;
                while((s = in.readLine()) != null) {
                    responseBody.append(s);
                    responseBody.append("\n");
                }

                statusMessage.setLength(0);
                statusMessage.append (activeConnection.getResponseMessage());
                // Look for a detailed error message from the server
                if (activeConnection.getHeaderField("Error") != null) {
                    statusMessage.append(": ");
                    String er = activeConnection.getHeaderField("Error");
                    if (serverErrorTranslations.containsKey(er)) er = serverErrorTranslations.get(er);
                    statusMessage.append(er);
                }
                activeConnection.disconnect();
                
                if (retCode != 200) {
                    throw new OsmTransferException(statusMessage.toString());
                }
                return responseBody.toString();
            } catch (UnknownHostException e) {
                throw new OsmTransferException(tr("Unknown host")+": "+e.getMessage(), e);
            } catch (SocketTimeoutException e) {
                if (retries-- > 0)
                    continue;
                throw new OsmTransferException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
            } catch (ConnectException e) {
                if (retries-- > 0)
                    continue;
                throw new OsmTransferException(e.getMessage()+ " " + e.getClass().getCanonicalName(), e);
            } catch (Exception e) {
                if (e instanceof OsmTransferException) throw (OsmTransferException) e;
                throw new OsmTransferException(e);
            }
        }
    }
}
