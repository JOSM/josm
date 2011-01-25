// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Map.Entry;
//import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.Base64;
import org.openstreetmap.josm.tools.XmlObjectParser;

/**
 * This class tweak the Preferences class to provide server side preference settings, as example
 * used in the applet version.
 *
 * @author Imi
 */
public class ServerSidePreferences extends Preferences {
    //static private final Logger logger = Logger.getLogger(ServerSidePreferences.class.getName());

    public class MissingPassword extends Exception{
        public String realm;
        public MissingPassword(String r) {
            realm = r;
        }
    }

    private final Connection connection;

    private class Connection extends OsmConnection {
        URL serverUrl;
        public Connection(URL serverUrl) {
            this.serverUrl = serverUrl;
        }
        public String download() throws MissingPassword {
            try {
                System.out.println("reading preferences from "+serverUrl);
                URLConnection con = serverUrl.openConnection();
                String username = get("applet.username");
                String password = get("applet.password");
                if(password.isEmpty() && username.isEmpty())
                    con.addRequestProperty("Authorization", "Basic "+Base64.encode(username+":"+password));
                con.connect();
                if(username.isEmpty() && con instanceof HttpURLConnection
                    && ((HttpURLConnection) con).getResponseCode()
                    == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    String t = ((HttpURLConnection) con).getHeaderField("WWW-Authenticate");
                    t = t.replace("Basic realm=\"","").replace("\"","");
                    throw new MissingPassword(t);
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder b = new StringBuilder();
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    b.append(line);
                    b.append("\n");
                }
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection) con).disconnect();
                }
                return b.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        public void upload(String s) {
            try {
                URL u = new URL(getPreferencesDir());
                System.out.println("uploading preferences to "+u);
                HttpURLConnection con = (HttpURLConnection)u.openConnection();
                String username = get("applet.username");
                String password = get("applet.password");
                if(password.isEmpty() && username.isEmpty())
                    con.addRequestProperty("Authorization", "Basic "+Base64.encode(username+":"+password));
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.connect();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(con.getOutputStream()));
                out.println(s);
                out.close();
                con.getInputStream().close();
                con.disconnect();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Preferences stored on {0}", u.getHost()),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Could not upload preferences. Reason: {0}", e.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public ServerSidePreferences(URL serverUrl) {
        Connection connection = null;
        try {
            connection = new Connection(new URL(serverUrl+"user/preferences"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Could not load preferences from server."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        this.connection = connection;
    }

    @Override public String getPreferencesDir() {
        return connection.serverUrl.toString();
    }

    /**
     * Do nothing on load. Preferences are loaded with download().
     */
    @Override public void load() {
    }

    /**
     * Do nothing on save. Preferences are uploaded using upload().
     */
    @Override public void save() {
    }

    public static class Prop {
        public String key;
        public String value;
    }

    public void download(String userName, String password) {
        if (!properties.containsKey("applet.username") && userName != null) {
            properties.put("applet.username", userName);
        }
        if (!properties.containsKey("applet.password") && password != null) {
            properties.put("applet.password", password);
        }
        try {
            download();
        } catch (MissingPassword e) {
        }
    }

    public boolean download() throws MissingPassword {
        resetToDefault();
        String cont = connection.download();
        if (cont == null) return false;
        Reader in = new StringReader(cont);
        boolean res = false;
        try {
            XmlObjectParser.Uniform<Prop> parser = new XmlObjectParser.Uniform<Prop>(in, "tag", Prop.class);
            for (Prop p : parser) {
                res = true;
                properties.put(p.key, p.value);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Use this instead of save() for the ServerSidePreferences, since uploads
     * are costly while save is called often.
     *
     * This is triggered by an explicit menu option.
     */
    public void upload() {
        StringBuilder b = new StringBuilder("<preferences>\n");
        for (Entry<String, String> p : properties.entrySet()) {
            if (p.getKey().equals("osm-server.password")) {
                continue; // do not upload password. It would get stored in plain!
            }
            b.append("<tag key='");
            b.append(XmlWriter.encode(p.getKey()));
            b.append("' value='");
            b.append(XmlWriter.encode(p.getValue()));
            b.append("' />\n");
        }
        b.append("</preferences>");
        connection.upload(b.toString());
    }
}
