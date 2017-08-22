// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.Logging;

/**
 * The preferences panel for parameters necessary for the Basic Authentication Scheme.
 * @since 2745
 */
public class BasicAuthenticationPreferencesPanel extends JPanel {

    /** the OSM user name */
    private final JosmTextField tfOsmUserName = new JosmTextField();
    /** the OSM password */
    private final JosmPasswordField tfOsmPassword = new JosmPasswordField();
    /** a panel with further information, e.g. some warnings */
    private JPanel decorationPanel;

    /**
     * Constructs a new {@code BasicAuthenticationPreferencesPanel}.
     */
    public BasicAuthenticationPreferencesPanel() {
        build();
    }

    /**
     * builds the UI
     */
    protected final void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        GridBagConstraints gc = new GridBagConstraints();

        // -- OSM user name
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 3, 3);
        add(new JLabel(tr("OSM username:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmUserName, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmUserName);
        UserNameValidator valUserName = new UserNameValidator(tfOsmUserName);
        valUserName.validate();

        // -- OSM password
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        add(new JLabel(tr("OSM password:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmPassword, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmPassword);
        tfOsmPassword.setToolTipText(tr("Please enter your OSM password"));

        // -- an info panel with a warning message
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(5, 0, 0, 0);
        gc.fill = GridBagConstraints.BOTH;
        decorationPanel = new JPanel(new BorderLayout());
        add(decorationPanel, gc);
    }

    /**
     * Inits contents from preferences.
     */
    public void initFromPreferences() {
        CredentialsAgent cm = CredentialsManager.getInstance();
        try {
            decorationPanel.removeAll();
            decorationPanel.add(cm.getPreferencesDecorationPanel(), BorderLayout.CENTER);
            PasswordAuthentication pa = cm.lookup(RequestorType.SERVER, OsmApi.getOsmApi().getHost());
            if (pa == null) {
                tfOsmUserName.setText("");
                tfOsmPassword.setText("");
            } else {
                tfOsmUserName.setText(pa.getUserName() == null ? "" : pa.getUserName());
                tfOsmPassword.setText(pa.getPassword() == null ? "" : String.valueOf(pa.getPassword()));
            }
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            Logging.warn(tr("Failed to retrieve OSM credentials from credential manager."));
            Logging.warn(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
            tfOsmUserName.setText("");
            tfOsmPassword.setText("");
        }
    }

    /**
     * Saves contents to preferences.
     */
    public void saveToPreferences() {
        CredentialsAgent cm = CredentialsManager.getInstance();
        try {
            PasswordAuthentication pa = new PasswordAuthentication(
                    tfOsmUserName.getText().trim(),
                    tfOsmPassword.getPassword()
            );
            cm.store(RequestorType.SERVER, OsmApi.getOsmApi().getHost(), pa);
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            Logging.warn(tr("Failed to save OSM credentials to credential manager."));
            Logging.warn(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
        }
    }

    /**
     * Clears the password field.
     */
    public void clearPassword() {
        tfOsmPassword.setText("");
    }
}
