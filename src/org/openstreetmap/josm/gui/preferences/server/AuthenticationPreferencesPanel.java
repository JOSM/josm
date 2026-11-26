// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is the preference panel for the authentication method and the authentication parameters.
 * @since 2745
 */
public class AuthenticationPreferencesPanel extends VerticallyScrollablePanel implements PropertyChangeListener {

    /** the choice of the authentication method; only shown to experts, see {@link #updateAuthenticationMethodVisibility()} */
    private final JPanel pnlAuthenticationMethod = new JPanel(new FlowLayout(FlowLayout.LEADING));
    /** follows expert mode; held here since {@link ExpertToggleAction} only keeps a weak reference to its listeners */
    private final transient ExpertToggleAction.ExpertModeChangeListener expertModeListener =
            isExpert -> updateAuthenticationMethodVisibility();
    /** indicates whether we use basic authentication */
    private final JRadioButton rbBasicAuthentication = new JRadioButton();
    /** indicates whether we use OAuth 2.0 as authentication scheme */
    private final JRadioButton rbOAuth20 = new JRadioButton();
    /** the panel which contains the authentication parameters for the respective authentication scheme */
    private final JPanel pnlAuthenticationParameters = new JPanel(new BorderLayout());
    /** the panel for the basic authentication parameters */
    private BasicAuthenticationPreferencesPanel pnlBasicAuthPreferences;
    /** the panel for the OAuth 2.0 authentication parameters */
    private OAuthAuthenticationPreferencesPanel pnlOAuth20Preferences;

    /**
     * Constructs a new {@code AuthenticationPreferencesPanel}.
     */
    public AuthenticationPreferencesPanel() {
        build();
        initFromPreferences();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#AuthenticationSettings"));
    }

    /**
     * builds the UI
     */
    protected final void build() {
        setLayout(new GridBagLayout());

        AuthenticationMethodChangeListener authChangeListener = new AuthenticationMethodChangeListener();

        // -- radio button for basic authentication
        pnlAuthenticationMethod.add(rbBasicAuthentication);
        rbBasicAuthentication.setText(tr("Use Basic Authentication"));
        rbBasicAuthentication.setToolTipText(tr("Select to use HTTP basic authentication with your OSM username and password"));
        rbBasicAuthentication.addItemListener(authChangeListener);
        //-- radio button for OAuth 2.0
        pnlAuthenticationMethod.add(rbOAuth20);
        rbOAuth20.setSelected(true); // This must before adding the listener; otherwise, saveToPreferences is called prior to initFromPreferences
        rbOAuth20.setText(tr("Use OAuth {0}", "2.0"));
        rbOAuth20.setToolTipText(tr("Select to use OAuth {0} as authentication mechanism", "2.0"));
        rbOAuth20.addItemListener(authChangeListener);

        add(pnlAuthenticationMethod, GBC.eol());
        ExpertToggleAction.addExpertModeChangeListener(expertModeListener);
        //-- radio button for OAuth
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbBasicAuthentication);
        bg.add(rbOAuth20);

        //-- add the panel which will hold the authentication parameters
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(0, 0, 0, 3);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(pnlAuthenticationParameters, gc);

        //-- the two panels for authentication parameters
        pnlBasicAuthPreferences = new BasicAuthenticationPreferencesPanel();
        pnlOAuth20Preferences = new OAuthAuthenticationPreferencesPanel(OAuthVersion.OAuth20);

        pnlAuthenticationParameters.add(pnlOAuth20Preferences, BorderLayout.CENTER);
        this.updateAcceptableAuthenticationMethods(OsmApi.getOsmApi().getServerUrl());
    }

    /**
     * Initializes the panel from preferences
     */
    public final void initFromPreferences() {
        final String authMethod = OsmApi.getAuthMethod();
        if ("basic".equals(authMethod)) {
            rbBasicAuthentication.setSelected(true);
        } else if ("oauth20".equals(authMethod)) {
            rbOAuth20.setSelected(true);
        } else {
            Logging.warn(
                    tr("Unsupported value in preference ''{0}'', got ''{1}''. Using authentication method ''OAuth 2.0 Authentication''.",
                            "osm-server.auth-method", authMethod));
            rbOAuth20.setSelected(true);
        }
        pnlBasicAuthPreferences.initFromPreferences();
        pnlOAuth20Preferences.initFromPreferences();
        updateAuthenticationMethodVisibility();
    }

    /**
     * Shows the choice of the authentication method in expert mode only, since basic authentication is meant as a
     * quick way to test one's own server rather than for normal use. It is shown regardless while basic
     * authentication is selected, so that a profile using it does not lose sight of its own setting. Selecting
     * another method does not hide the choice straight away, which would take the option away under the mouse.
     */
    private void updateAuthenticationMethodVisibility() {
        pnlAuthenticationMethod.setVisible(ExpertToggleAction.isExpert() || rbBasicAuthentication.isSelected());
    }

    /**
     * Saves the current values to the preferences
     */
    public final void saveToPreferences() {
        // save the authentication method
        String authMethod;
        if (rbBasicAuthentication.isSelected()) {
            authMethod = "basic";
        } else if (rbOAuth20.isSelected()) {
            authMethod = "oauth20";
        } else {
            throw new IllegalStateException("One of OAuth 2.0 or Basic authentication must be checked");
        }
        final boolean initUser = Config.getPref().put("osm-server.auth-method", authMethod);
        if ("basic".equals(authMethod)) {
            // save username and password and clear the OAuth token
            pnlBasicAuthPreferences.saveToPreferences();
            OAuthAccessTokenHolder.getInstance().clear();
            OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
        } else if ("oauth20".equals(authMethod)) {
            // oauth20
            // clear the password in the preferences
            pnlBasicAuthPreferences.clearPassword();
            pnlOAuth20Preferences.saveToPreferences();
        }
        if (initUser) {
            if ("basic".equals(authMethod)) {
                UserIdentityManager.getInstance().initFromPreferences();
            } else if (OsmApi.isUsingOAuthAndOAuthSetUp(OsmApi.getOsmApi())) {
                UserIdentityManager.getInstance().initFromOAuth();
            } else {
                UserIdentityManager.getInstance().setAnonymous();
            }
        }
    }

    /**
     * Listens to changes in the authentication method
     */
    class AuthenticationMethodChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            pnlAuthenticationParameters.removeAll();
            if (rbBasicAuthentication.isSelected()) {
                pnlAuthenticationParameters.add(pnlBasicAuthPreferences, BorderLayout.CENTER);
                pnlBasicAuthPreferences.revalidate();
            } else if (rbOAuth20.isSelected()) {
                pnlAuthenticationParameters.add(pnlOAuth20Preferences, BorderLayout.CENTER);
                // only refresh it: saving here would save what was entered before the user pressed OK
                pnlOAuth20Preferences.initFromPreferences();
                pnlOAuth20Preferences.revalidate();
            }
            repaint();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (pnlOAuth20Preferences != null) {
            pnlOAuth20Preferences.propertyChange(evt);
        }
        if (OsmApiUrlInputPanel.API_URL_PROP.equals(evt.getPropertyName())) {
            this.updateAcceptableAuthenticationMethods((String) evt.getNewValue());
        }
    }

    /**
     * Update the acceptable authentications methods: basic authentication is not offered for a server which is known
     * not to accept it, unless it is in use already, so that it can be changed.
     * @param apiUrl The API url to check
     */
    private void updateAcceptableAuthenticationMethods(String apiUrl) {
        final String authMethod = OsmApi.getAuthMethod();
        rbBasicAuthentication.setEnabled(rbBasicAuthentication.isSelected() || "basic".equals(authMethod)
                || !isOAuthOnly(apiUrl));
    }

    /**
     * Determines whether a server is known to accept OAuth 2.0 only, not basic authentication.
     * @param apiUrl The API url to check
     * @return {@code true} for the default OSM API and the OpenGeofiction API
     */
    private static boolean isOAuthOnly(String apiUrl) {
        final String url = OsmApi.normalizeApiUrl(apiUrl);
        return JosmUrls.getInstance().getDefaultOsmApiUrl().equals(url) || "https://opengeofiction.net/api".equals(url);
    }

}
