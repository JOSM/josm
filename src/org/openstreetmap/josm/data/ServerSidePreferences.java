// License: GPL. For details, see LICENSE file.
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

import javax.swing.JOptionPane;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.tools.Base64;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class tweak the Preferences class to provide server side preference settings, as example
 * used in the applet version.
 *
 * @author Imi
 */
public class ServerSidePreferences extends Preferences {
    public static class MissingPassword extends Exception{
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
                Main.info("reading preferences from "+serverUrl);
                URLConnection con = serverUrl.openConnection();
                String username = get("applet.username");
                String password = get("applet.password");
                if(password.isEmpty() && username.isEmpty()) {
                    con.addRequestProperty("Authorization", "Basic "+Base64.encode(username+":"+password));
                }
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
                try {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        b.append(line);
                        b.append("\n");
                    }
                } finally {
                    reader.close();
                }
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection) con).disconnect();
                }
                return b.toString();
            } catch (IOException e) {
                Main.error(e);
            }
            return null;
        }
        public void upload(String s) {
            try {
                URL u = new URL(getPreferencesDir());
                Main.info("uploading preferences to "+u);
                HttpURLConnection con = (HttpURLConnection)u.openConnection();
                String username = get("applet.username");
                String password = get("applet.password");
                if(password.isEmpty() && username.isEmpty()) {
                    con.addRequestProperty("Authorization", "Basic "+Base64.encode(username+":"+password));
                }
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.connect();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(con.getOutputStream()));
                out.println(s);
                Utils.close(out);
                Utils.close(con.getInputStream());
                con.disconnect();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Preferences stored on {0}", u.getHost()),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                        );
            } catch (Exception e) {
                Main.error(e);
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
            Main.error(e);
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

    public void download(String userName, String password) {
        if (!settingsMap.containsKey("applet.username") && userName != null) {
            settingsMap.put("applet.username", new StringSetting(userName));
        }
        if (!settingsMap.containsKey("applet.password") && password != null) {
            settingsMap.put("applet.password", new StringSetting(password));
        }
        try {
            download();
        } catch (MissingPassword e) {
            Main.warn(e);
        }
    }

    public boolean download() throws MissingPassword {
        resetToDefault();
        String cont = connection.download();
        if (cont == null) return false;
        Reader in = new StringReader(cont);
        boolean res = false;
        try {
            fromXML(in);
        } catch (RuntimeException e) {
            Main.error(e);
        } catch (XMLStreamException e) {
            Main.error(e);
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
        connection.upload(toXML(true));
    }
}
