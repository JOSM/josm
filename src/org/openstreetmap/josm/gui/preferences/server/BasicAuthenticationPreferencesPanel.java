// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.PasswordAuthentication;
import java.net.Authenticator.RequestorType;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.CredentialsManagerException;
import org.openstreetmap.josm.io.auth.CredentialsManagerFactory;
import org.openstreetmap.josm.io.auth.JosmPreferencesCredentialManager;

/**
 * The preferences panel for parameters necessary for the Basic Authentication
 * Scheme.
 * 
 */
public class BasicAuthenticationPreferencesPanel extends JPanel {

    /** the OSM user name */
    private JTextField tfOsmUserName;
    private UserNameValidator valUserName;
    /** the OSM password */
    private JPasswordField tfOsmPassword;


    /**
     * builds the UI
     */
    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        GridBagConstraints gc = new GridBagConstraints();

        // -- OSM user name
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,3,3);
        add(new JLabel(tr("OSM username:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmUserName = new JTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmUserName);
        valUserName = new UserNameValidator(tfOsmUserName);
        valUserName.validate();

        // -- OSM password
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        add(new JLabel(tr("OSM password:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmPassword = new JPasswordField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmPassword);
        tfOsmPassword.setToolTipText(tr("Please enter your OSM password"));

        // -- an info panel with a warning message
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(5,0,0,0);
        gc.fill = GridBagConstraints.BOTH;
        HtmlPanel pnlMessage = new HtmlPanel();
        HTMLEditorKit kit = (HTMLEditorKit)pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(".warning-body {background-color:rgb(253,255,221);padding: 10pt; border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        pnlMessage.setText(
                tr(
                        "<html><body>"
                        + "<p class=\"warning-body\">"
                        + "<strong>Warning:</strong> The password is stored in plain text in the JOSM preferences file. "
                        + "Furthermore, it is transferred <strong>unencrypted</strong> in every request sent to the OSM server. "
                        + "<strong>Do not use a valuable password.</strong>"
                        + "</p>"
                        + "</body></html>"
                )
        );
        add(pnlMessage, gc);
    }

    public BasicAuthenticationPreferencesPanel() {
        build();
    }

    public void initFromPreferences() {
        CredentialsManager cm = CredentialsManagerFactory.getCredentialManager();
        try {
            PasswordAuthentication pa = cm.lookup(RequestorType.SERVER);
            if (pa == null) {
                tfOsmUserName.setText("");
                tfOsmPassword.setText("");
            } else {
                tfOsmUserName.setText(pa.getUserName() == null? "" : pa.getUserName());
                tfOsmPassword.setText(pa.getPassword() == null ? "" : String.valueOf(pa.getPassword()));
            }
        } catch(CredentialsManagerException e) {
            e.printStackTrace();
            System.err.println(tr("Warning: failed to retrieve OSM credentials from credential manager."));
            System.err.println(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
            tfOsmUserName.setText("");
            tfOsmPassword.setText("");
        }
    }

    public void saveToPreferences() {
        CredentialsManager cm = CredentialsManagerFactory.getCredentialManager();
        try {
            PasswordAuthentication pa = new PasswordAuthentication(
                    tfOsmUserName.getText().trim(),
                    tfOsmPassword.getPassword()
            );
            cm.store(RequestorType.SERVER, pa);
            // always save the username to the preferences if it isn't already saved there
            // by the credential manager
            if (! (cm instanceof JosmPreferencesCredentialManager)) {
                Main.pref.put("osm-server.username", tfOsmUserName.getText().trim());
            }
        } catch(CredentialsManagerException e) {
            e.printStackTrace();
            System.err.println(tr("Warning: failed to save OSM credentials to credential manager."));
            System.err.println(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
        }
    }

    public void clearPassword() {
        tfOsmPassword.setText("");
    }
}
