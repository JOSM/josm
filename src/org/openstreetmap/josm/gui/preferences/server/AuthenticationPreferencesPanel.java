// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.auth.CredentialsManager;

/**
 * This is the preference panel for the authentication method and the authentication
 * parameters.
 *
 */
public class AuthenticationPreferencesPanel extends VerticallyScrollablePanel implements PropertyChangeListener{

    /** indicates whether we use basic authentication */
    private JRadioButton rbBasicAuthentication;
    /** indicates whether we use OAuth as authentication scheme */
    private JRadioButton rbOAuth;
    /** the panel which contains the authentication parameters for the respective
     * authentication scheme
     */
    private JPanel pnlAuthenticationParameteters;
    /** the panel for the basic authentication parameters */
    private BasicAuthenticationPreferencesPanel pnlBasicAuthPreferences;
    /** the panel for the OAuth authentication parameters */
    private OAuthAuthenticationPreferencesPanel pnlOAuthPreferences;
    /** the panel for messages notifier preferences */
    private FeaturesPanel pnlFeaturesPreferences;

    /**
     * builds the UI
     */
    protected final void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        AuthenticationMethodChangeListener authChangeListener = new AuthenticationMethodChangeListener();

        // -- radio button for basic authentication
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0, 3);
        add(rbBasicAuthentication = new JRadioButton(), gc);
        rbBasicAuthentication.setText(tr("Use Basic Authentication"));
        rbBasicAuthentication.setToolTipText(tr("Select to use HTTP basic authentication with your OSM username and password"));
        rbBasicAuthentication.addItemListener(authChangeListener);

        //-- radio button for OAuth
        gc.gridx = 1;
        gc.weightx = 1.0;
        add(rbOAuth = new JRadioButton(), gc);
        rbOAuth.setText(tr("Use OAuth"));
        rbOAuth.setToolTipText(tr("Select to use OAuth as authentication mechanism"));
        rbOAuth.addItemListener(authChangeListener);

        //-- radio button for OAuth
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbBasicAuthentication);
        bg.add(rbOAuth);

        //-- add the panel which will hold the authentication parameters
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnlAuthenticationParameteters = new JPanel();
        add(pnlAuthenticationParameteters, gc);
        pnlAuthenticationParameteters.setLayout(new BorderLayout());

        //-- the two panels for authentication parameters
        pnlBasicAuthPreferences = new BasicAuthenticationPreferencesPanel();
        pnlOAuthPreferences = new OAuthAuthenticationPreferencesPanel();

        rbBasicAuthentication.setSelected(true);
        pnlAuthenticationParameteters.add(pnlBasicAuthPreferences, BorderLayout.CENTER);

        gc.gridy = 2;
        add(new JSeparator(), gc);

        //-- the panel for API feature preferences
        gc.gridy = 3;
        gc.fill = GridBagConstraints.NONE;
        pnlFeaturesPreferences = new FeaturesPanel();
        add(pnlFeaturesPreferences, gc);
    }

    /**
     * Constructs a new {@code AuthenticationPreferencesPanel}.
     */
    public AuthenticationPreferencesPanel() {
        build();
        initFromPreferences();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#AuthenticationSettings"));
    }

    /**
     * Initializes the panel from preferences
     */
    public final void initFromPreferences() {
        String authMethod = Main.pref.get("osm-server.auth-method", "basic");
        if ("basic".equals(authMethod)) {
            rbBasicAuthentication.setSelected(true);
        } else if ("oauth".equals(authMethod)) {
            rbOAuth.setSelected(true);
        } else {
            Main.warn(tr("Unsupported value in preference ''{0}'', got ''{1}''. Using authentication method ''Basic Authentication''.", "osm-server.auth-method", authMethod));
            rbBasicAuthentication.setSelected(true);
        }
        pnlBasicAuthPreferences.initFromPreferences();
        pnlOAuthPreferences.initFromPreferences();
        pnlFeaturesPreferences.initFromPreferences();
    }

    /**
     * Saves the current values to preferences
     */
    public final void saveToPreferences() {
        // save the authentication method
        String authMethod;
        if (rbBasicAuthentication.isSelected()) {
            authMethod = "basic";
        } else {
            authMethod = "oauth";
        }
        Main.pref.put("osm-server.auth-method", authMethod);
        if ("basic".equals(authMethod)) {
            // save username and password and clear the OAuth token
            pnlBasicAuthPreferences.saveToPreferences();
            OAuthAccessTokenHolder.getInstance().clear();
            OAuthAccessTokenHolder.getInstance().save(Main.pref, CredentialsManager.getInstance());
        } else if ("oauth".equals(authMethod)) {
            // clear the password in the preferences
            pnlBasicAuthPreferences.clearPassword();
            pnlBasicAuthPreferences.saveToPreferences();
            pnlOAuthPreferences.saveToPreferences();
        }
        // save message notifications preferences. To be done after authentication preferences.
        pnlFeaturesPreferences.saveToPreferences();
    }

    /**
     * Listens to changes in the authentication method
     */
    class AuthenticationMethodChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (rbBasicAuthentication.isSelected()) {
                pnlAuthenticationParameteters.removeAll();
                pnlAuthenticationParameteters.add(pnlBasicAuthPreferences, BorderLayout.CENTER);
                pnlBasicAuthPreferences.revalidate();
            } else {
                pnlAuthenticationParameteters.removeAll();
                pnlAuthenticationParameteters.add(pnlOAuthPreferences, BorderLayout.CENTER);
                pnlOAuthPreferences.revalidate();
            }
            repaint();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (pnlOAuthPreferences != null) {
            pnlOAuthPreferences.propertyChange(evt);
        }
    }
}
