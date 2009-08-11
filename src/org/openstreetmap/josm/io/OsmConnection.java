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
import org.openstreetmap.josm.io.CredentialsManager.CMException;

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
                auth = credentialsManager.lookupUsername() + ":" + credentialsManager.lookupPassword();
            }
        } catch (CMException e) {
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

    public static class PlainCredentialsManager implements CredentialsManager {
        public String lookupUsername() throws CMException {
            String username = Main.pref.get("osm-server.username", null);
            if (username == null) throw new CredentialsManager.NoContentException();
            return username;
        }
        public String lookupPassword() throws CMException {
            String password = Main.pref.get("osm-server.password");
            if (password == null) throw new CredentialsManager.NoContentException();
            return password;
        }
        public void storeUsername(String username) {
            Main.pref.put("osm-server.username", username);
        }
        public void storePassword(String password) {
            Main.pref.put("osm-server.password", password);
        }
        public PasswordAuthentication getPasswordAuthentication(OsmAuth caller) {
            String username, password;
            try {
                username = lookupUsername();
            } catch (CMException e) {
                username = "";
            }
            try {
                password = lookupPassword();
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

                JCheckBox savePassword = new JCheckBox(tr("Save user and password (unencrypted)"), !username.equals("") && !password.equals(""));
                p.add(savePassword, GBC.eop());

                int choice = new ExtendedDialog(Main.parent,
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
                    storeUsername(username);
                    storePassword(password);
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
                 * Editfield for the username to the OSM account.
                 */
                private JTextField osmDataUsername = new JTextField(20);
                /**
                 * Passwordfield for the userpassword of the REST API.
                 */
                private JPasswordField osmDataPassword = new JPasswordField(20);

                private String oldUsername = "";
                private String oldPassword = "";

                public void addPreferenceOptions(JPanel panel) {
                    try {
                        oldUsername = lookupUsername();
                    } catch (CMException e) {
                        oldUsername = "";
                    }
                    try {
                        oldPassword = lookupPassword();
                    } catch (CMException e) {
                        oldPassword = "";
                    }
                    osmDataUsername.setText(oldUsername);
                    osmDataPassword.setText(oldPassword);
                    osmDataUsername.setToolTipText(tr("Login name (e-mail) to the OSM account."));
                    osmDataPassword.setToolTipText(tr("Login password to the OSM account. Leave blank to not store any password."));
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
                    String newUsername = osmDataUsername.getText();
                    String newPassword = String.valueOf(osmDataPassword.getPassword());
                    if (!oldUsername.equals(newUsername))
                        storeUsername(newUsername);
                    if (!oldPassword.equals(newPassword))
                        storePassword(newPassword);
                }
            };
        }
    }
}
