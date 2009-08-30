// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Base64;
import org.openstreetmap.josm.tools.GBC;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {

    protected boolean cancel = false;
    protected HttpURLConnection activeConnection;
    /**
     * Handles password storage and some related gui-components.
     * It can be set by a plugin. This may happen at startup and
     * by changing the preferences.
     * Syncronize on this object to get or set a consistent
     * username/password pair.
     */
    public static CredentialsManager credentialsManager = new PlainCredentialsManager();

    private static OsmAuth authentication = new OsmAuth();

    /**
     * Initialize the http defaults and the authenticator.
     */
    static {
        // TODO: current authentication handling is sub-optimal in that it seems to use the same authenticator for
        // any kind of request. HTTP requests executed by plugins, e.g. to password-protected WMS servers,
        // will use the same username/password which is undesirable.
        try {
            HttpURLConnection.setFollowRedirects(true);
            Authenticator.setDefault(authentication);
        } catch (SecurityException e) {
        }
    }

    /**
     * The authentication class handling the login requests.
     */
    public static class OsmAuth extends Authenticator {
        /**
         * Set to true, when the autenticator tried the password once.
         */
        public boolean passwordtried = false;
        /**
         * Whether the user cancelled the password dialog
         */
        public boolean authCancelled = false;
        @Override protected PasswordAuthentication getPasswordAuthentication() {
            return credentialsManager.getPasswordAuthentication(this);
        }
    }

    /**
     * Must be called before each connection attemp to initialize the authentication.
     */
    protected final void initAuthentication() {
        authentication.authCancelled = false;
        authentication.passwordtried = false;
    }

    /**
     * @return Whether the connection was cancelled.
     */
    protected final boolean isAuthCancelled() {
        return authentication.authCancelled;
    }

    public void cancel() {
        //TODO
        //Main.pleaseWaitDlg.currentAction.setText(tr("Aborting..."));
        cancel = true;
        if (activeConnection != null) {
            activeConnection.setConnectTimeout(100);
            activeConnection.setReadTimeout(100);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {}
            activeConnection.disconnect();
        }
    }

    protected void addAuth(HttpURLConnection con) throws CharacterCodingException {
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        String auth;
        try {
            synchronized (credentialsManager) {
                auth = credentialsManager.lookup(CredentialsManager.Key.USERNAME) + ":" + 
                    credentialsManager.lookup(CredentialsManager.Key.PASSWORD);
            }
        } catch (CredentialsManager.CMException e) {
            auth = ":";
        }
        ByteBuffer bytes = encoder.encode(CharBuffer.wrap(auth));
        con.addRequestProperty("Authorization", "Basic "+Base64.encode(bytes));
    }

    /**
     * Replies true if this connection is canceled
     * 
     * @return true if this connection is canceled
     * @return
     */
    public boolean isCanceled() {
        return cancel;
    }
    /**
     * Default implementation of the CredentialsManager interface.
     * Saves passwords in plain text file.
     */
    public static class PlainCredentialsManager implements CredentialsManager {
        public String lookup(CredentialsManager.Key key) throws CMException {
            String secret = Main.pref.get("osm-server." + key.toString(), null);
            if (secret == null) throw new CredentialsManager.NoContentException();
            return secret;            
        }
        public void store(CredentialsManager.Key key, String secret) {
            Main.pref.put("osm-server." + key.toString(), secret);
        }
        public PasswordAuthentication getPasswordAuthentication(OsmAuth caller) {
            String username, password;
            try {
                username = lookup(Key.USERNAME);
            } catch (CMException e) {
                username = "";
            }
            try {
                password = lookup(Key.PASSWORD);
            } catch (CMException e) {
                password = "";
            }
            if (caller.passwordtried || username.equals("") || password.equals("")) {
                JPanel p = new JPanel(new GridBagLayout());
                if (!username.equals("") && !password.equals("")) {
                    p.add(new JLabel(tr("Incorrect password or username.")), GBC.eop());
                }
                p.add(new JLabel(tr("Username")), GBC.std().insets(0,0,10,0));
                JTextField usernameField = new JTextField(username, 20);
                p.add(usernameField, GBC.eol());
                p.add(new JLabel(tr("Password")), GBC.std().insets(0,0,10,0));
                JPasswordField passwordField = new JPasswordField(password, 20);
                p.add(passwordField, GBC.eol());
                JLabel warning = new JLabel(tr("Warning: The password is transferred unencrypted."));
                warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
                p.add(warning, GBC.eop());

                JCheckBox savePassword = new JCheckBox(tr("Save user and password (unencrypted)"), 
                                                       !username.equals("") && !password.equals(""));
                p.add(savePassword, GBC.eop());

                int choice = new ExtendedDialog(
                    Main.parent,
                    tr("Enter Password"),
                    p,
                    new String[] {tr("Login"), tr("Cancel")},
                    new String[] {"ok.png", "cancel.png"}).getValue();

                if (choice != 1) {
                    caller.authCancelled = true;
                    return null;
                }
                username = usernameField.getText();
                password = String.valueOf(passwordField.getPassword());
                if (savePassword.isSelected()) {
                    store(Key.USERNAME, username);
                    store(Key.PASSWORD, password);
                }
                if (username.equals(""))
                    return null;
            }
            caller.passwordtried = true;
            return new PasswordAuthentication(username, password.toCharArray());
        }
        public PreferenceAdditions newPreferenceAdditions() {
            return new PreferenceAdditions() {
                /**
                 * Editfield for the Base url to the REST API from OSM.
                 */
                final private JTextField osmDataServerURL = new JTextField(20);
                /**
                 * Editfield for the username to the OSM account.
                 */
                final private JTextField osmDataUsername = new JTextField(20);
                /**
                 * Passwordfield for the userpassword of the REST API.
                 */
                final private JPasswordField osmDataPassword = new JPasswordField(20);

                private String oldServerURL = "";
                private String oldUsername = "";
                private String oldPassword = "";

                public void addPreferenceOptions(JPanel panel) {
                    try {
                        oldServerURL = lookup(Key.OSM_SERVER_URL); // result is not null (see CredentialsManager)
                    } catch (CMException e) {
                        oldServerURL = "";
                    }
                    if (oldServerURL.equals("")) oldServerURL = "http://api.openstreetmap.org/api";
                    try {
                        oldUsername = lookup(Key.USERNAME);
                    } catch (CMException e) {
                        oldUsername = "";
                    }
                    try {
                        oldPassword = lookup(Key.PASSWORD);
                    } catch (CMException e) {
                        oldPassword = "";
                    }
                    osmDataServerURL.setText(oldServerURL);
                    osmDataUsername.setText(oldUsername);
                    osmDataPassword.setText(oldPassword);
                    osmDataServerURL.setToolTipText(tr("The base URL for the OSM server (REST API)"));
                    osmDataUsername.setToolTipText(tr("Login name (e-mail) to the OSM account."));
                    osmDataPassword.setToolTipText(tr("Login password to the OSM account. Leave blank to not store any password."));
                    panel.add(new JLabel(tr("Base Server URL")), GBC.std());
                    panel.add(osmDataServerURL, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
                    panel.add(new JLabel(tr("OSM username (e-mail)")), GBC.std());
                    panel.add(osmDataUsername, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
                    panel.add(new JLabel(tr("OSM password")), GBC.std());
                    panel.add(osmDataPassword, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,0));
                    JLabel warning = new JLabel(tr("<html>" +
                            "WARNING: The password is stored in plain text in the preferences file.<br>" +
                            "The password is transferred in plain text to the server, encoded in the URL.<br>" +
                    "<b>Do not use a valuable Password.</b></html>"));
                    warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
                    panel.add(warning, GBC.eop().fill(GBC.HORIZONTAL));
                }
                public void preferencesChanged() {
                    String newServerURL = osmDataServerURL.getText();
                    String newUsername = osmDataUsername.getText();
                    String newPassword = String.valueOf(osmDataPassword.getPassword());
                    if (!oldServerURL.equals(newServerURL)) {
                        store(Key.OSM_SERVER_URL, newServerURL); 
                    }
                    if (!oldUsername.equals(newUsername)) {
                        store(Key.USERNAME, newUsername);
                    }
                    if (!oldPassword.equals(newPassword)) {
                        store(Key.PASSWORD, newPassword);
                    }
                }
            };
        }
    }
}
