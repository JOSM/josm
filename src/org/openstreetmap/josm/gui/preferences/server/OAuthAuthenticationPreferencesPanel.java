// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Authorization;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.oauth.osm.OsmScopes;
import org.openstreetmap.josm.data.validation.routines.DomainValidator;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.oauth.AdvancedOAuthPropertiesPanel;
import org.openstreetmap.josm.gui.oauth.TestAccessTokenTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * The preferences panel for OAuth 2.0: whether JOSM has an Access Token for the server, a button to authorize JOSM
 * in the web browser, and the advanced OAuth parameters, which include the Access Token itself so that a token
 * obtained outside JOSM can be pasted instead.
 * @since 2745
 */
public class OAuthAuthenticationPreferencesPanel extends JPanel implements PropertyChangeListener {
    private final JCheckBox cbUseForAllRequests = new JCheckBox();
    private final JCheckBox cbSaveToPreferences = new JCheckBox(tr("Save to preferences"));
    private final JPanel pnlAuthorisationMessage = new JPanel(new BorderLayout());
    private final NotYetAuthorisedPanel pnlNotYetAuthorised;
    private final AdvancedOAuthPropertiesPanel pnlAdvancedProperties;
    private final AlreadyAuthorisedPanel pnlAlreadyAuthorised;
    private final OAuthVersion oAuthVersion;
    private String apiUrl;

    /**
     * Create the panel.
     * @param oAuthVersion The OAuth version to use
     */
    public OAuthAuthenticationPreferencesPanel(OAuthVersion oAuthVersion) {
        this.oAuthVersion = oAuthVersion;
        // These must come after we set the oauth version
        this.pnlAdvancedProperties = new AdvancedOAuthPropertiesPanel(this.oAuthVersion);
        this.pnlNotYetAuthorised = new NotYetAuthorisedPanel();
        this.pnlAlreadyAuthorised = new AlreadyAuthorisedPanel();
        build();
    }

    /**
     * Builds the panel for entering the advanced OAuth parameters
     *
     * @return panel with advanced settings
     */
    protected JPanel buildAdvancedPropertiesPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        cbUseForAllRequests.setText(tr("Use OAuth for all requests to {0}", OsmApi.getOsmApi().getServerUrl()));
        cbUseForAllRequests.setToolTipText(tr("For user-based bandwidth limit instead of IP-based one"));
        pnl.add(cbUseForAllRequests, GBC.eol().fill(GBC.HORIZONTAL));

        pnl.add(pnlAdvancedProperties, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 3, 0, 0));
        pnlAdvancedProperties.initialize(OsmApi.getOsmApi().getServerUrl());
        pnlAdvancedProperties.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );
        pnlAdvancedProperties.addPropertyChangeListener(this);
        return pnl;
    }

    /**
     * Re-evaluate whether the "Authorize now" buttons can be enabled, after the API URL or the
     * advanced OAuth parameters changed.
     */
    private void updateAuthoriseNowActions() {
        for (JPanel panel : Arrays.asList(this.pnlNotYetAuthorised, this.pnlAlreadyAuthorised.pnlButtons)) {
            for (Component component : panel.getComponents()) {
                if (component instanceof JButton && ((JButton) component).getAction() instanceof AuthoriseNowAction) {
                    ((AuthoriseNowAction) ((JButton) component).getAction()).updateEnabledState();
                }
            }
        }
    }

    /**
     * builds the UI
     */
    protected final void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // the panel for the OAuth parameters. pnlAuthorisationMessage is an
        // empty panel. It is going to be filled later, depending on the
        // current OAuth state in JOSM.
        add(pnlAuthorisationMessage, GBC.eol().fill(GridBagConstraints.BOTH).anchor(GridBagConstraints.NORTHWEST)
                .weight(1, 1).insets(0, 10, 0, 0));
        // the panel with the advanced options
        add(buildAdvancedPropertiesPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
    }

    protected void refreshView() {
        pnlAuthorisationMessage.removeAll();
        pnlAdvancedProperties.setAccessToken(getBearerToken());
        if (OAuthAccessTokenHolder.getInstance().getAccessToken(this.apiUrl, this.oAuthVersion) != null) {
            pnlAuthorisationMessage.add(pnlAlreadyAuthorised, BorderLayout.CENTER);
            pnlAlreadyAuthorised.refreshView();
            pnlAlreadyAuthorised.revalidate();
        } else {
            pnlAuthorisationMessage.add(pnlNotYetAuthorised, BorderLayout.CENTER);
            pnlNotYetAuthorised.revalidate();
        }
        repaint();
    }

    /**
     * Get the bearer token JOSM currently has for the API.
     * @return the bearer token, or {@code null} if there is none
     */
    private String getBearerToken() {
        if (apiUrl == null) {
            return null;
        }
        final IOAuthToken token = OAuthAccessTokenHolder.getInstance().getAccessToken(apiUrl, oAuthVersion);
        return token instanceof OAuth20Token ? ((OAuth20Token) token).getBearerToken() : null;
    }

    /**
     * Determines whether the user changed the access token in the advanced OAuth parameters.
     * @return {@code true} if the access token entered differs from the one JOSM has
     */
    private boolean isAccessTokenEdited() {
        final String current = getBearerToken();
        return apiUrl != null && !pnlAdvancedProperties.getAccessToken().equals(current == null ? "" : current);
    }

    /**
     * Get the access token entered in the advanced OAuth parameters: a token issued outside JOSM, pasted instead of
     * authorizing JOSM. It works with nothing else, so it is a way to authenticate when authorizing JOSM is not
     * possible.
     * @return the token, or {@code null} if none is entered
     */
    private IOAuthToken getEnteredAccessToken() {
        final String entered = pnlAdvancedProperties.getAccessToken();
        return entered.isEmpty() ? null : OAuth20Token.fromAccessToken(pnlAdvancedProperties.getDisplayedParameters(), entered);
    }

    /**
     * Takes over the access token entered in the advanced OAuth parameters, or removes the token if the field was
     * cleared. This is done when the preferences are saved, so that cancelling them leaves the token as it was. A
     * token which was not changed is left as it is, so that for example its refresh token is kept.
     */
    private void applyEnteredAccessToken() {
        if (isAccessTokenEdited()) {
            OAuthAccessTokenHolder.getInstance().setAccessToken(apiUrl, getEnteredAccessToken());
        }
    }

    /**
     * Sets the URL of the OSM API for which this panel is currently displaying OAuth properties.
     *
     * @param apiUrl the api URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        pnlAdvancedProperties.setApiUrl(apiUrl);
        updateAuthoriseNowActions();
    }

    /**
     * Initializes the panel from preferences
     */
    public void initFromPreferences() {
        setApiUrl(OsmApi.getOsmApi().getServerUrl().trim());
        cbUseForAllRequests.setSelected(OsmApi.USE_OAUTH_FOR_ALL_REQUESTS.get());
        refreshView();
    }

    /**
     * Saves the current values to preferences
     */
    public void saveToPreferences() {
        applyEnteredAccessToken();
        OAuthAccessTokenHolder.getInstance().setSaveToPreferences(cbSaveToPreferences.isSelected());
        OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
        OsmApi.USE_OAUTH_FOR_ALL_REQUESTS.put(cbUseForAllRequests.isSelected());
        pnlAdvancedProperties.rememberPreferences();
    }

    /**
     * The preferences panel displayed if there is currently no Access Token available.
     * This means that the user didn't run through the OAuth authorisation procedure yet.
     *
     */
    private class NotYetAuthorisedPanel extends JPanel {
        /**
         * Constructs a new {@code NotYetAuthorisedPanel}.
         */
        NotYetAuthorisedPanel() {
            build();
        }

        protected void build() {
            setLayout(new GridBagLayout());

            // A message explaining that the user isn't authorised yet
            JMultilineLabel lbl = new JMultilineLabel(
                    tr("You do not have an Access Token yet to access the OSM server using OAuth. Please authorize first."));
            add(lbl, GBC.eol().anchor(GBC.NORTHWEST).fill(GBC.HORIZONTAL));
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));

            // Action for authorising now
            add(new JButton(new AuthoriseNowAction()), GBC.eol());

            // filler - grab remaining space
            add(new JPanel(), GBC.std().fill(GBC.BOTH));
        }
    }

    /**
     * The preferences panel displayed if there is currently an AccessToken available.
     *
     */
    private class AlreadyAuthorisedPanel extends JPanel {
        private final JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        /**
         * Constructs a new {@code AlreadyAuthorisedPanel}.
         */
        AlreadyAuthorisedPanel() {
            build();
            refreshView();
        }

        protected void build() {
            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(0, 0, 3, 3);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridwidth = 2;
            JMultilineLabel lbl = new JMultilineLabel(tr("You already have an Access Token to access the OSM server using OAuth."));
            add(lbl, gc);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));

            // -- save to preferences; the Access Token itself is shown with the advanced OAuth parameters
            gc.gridy = 1;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(cbSaveToPreferences, gc);
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

            // -- action buttons
            pnlButtons.add(new JButton(new RenewAuthorisationAction()));
            pnlButtons.add(new JButton(new TestAuthorisationAction()));
            pnlButtons.add(new JButton(new RemoveAuthorisationAction()));
            gc.gridy = 2;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(pnlButtons, gc);

            // filler - grab the remaining space
            gc.gridy = 3;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }

        protected final void refreshView() {
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());
        }
    }

    /**
     * Action to authorise the current user: JOSM is authorized in the web browser, with the OAuth parameters
     * currently shown, and receives an Access Token by remote control.
     */
    private class AuthoriseNowAction extends AbstractAction {
        /** Counts the updates of the enabled state, so that the result of an earlier one cannot override a later one */
        private int updates;

        AuthoriseNowAction() {
            putValue(NAME, tr("Authorize now"));
            putValue(SHORT_DESCRIPTION, tr("Authorize JOSM in the web browser to access the OSM server on your behalf, "
                    + "and get an Access Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            updateEnabledState();
        }

        void updateEnabledState() {
            final int update = ++updates;
            if (!pnlAdvancedProperties.isUseDefaultSettings()) {
                // The user supplied the parameters - the client id of an OAuth application they
                // registered on the server themselves. Authorization uses exactly these parameters
                // when the button is pressed, so they decide whether it can be pressed.
                this.setEnabled(!Utils.isEmpty(pnlAdvancedProperties.getEnteredClientId()));
            } else if (Utils.isValidUrl(apiUrl)) {
                final URI apiURI;
                try {
                    apiURI = new URI(apiUrl);
                } catch (URISyntaxException e) {
                    Logging.trace(e);
                    return;
                }
                if (DomainValidator.getInstance().isValid(apiURI.getHost())) {
                    // We want to avoid trying to make connection with an invalid URL. These are the parameters
                    // authorization uses, see AdvancedOAuthPropertiesPanel.getAdvancedParameters().
                    final String currentApiUrl = apiUrl;
                    MainApplication.worker.execute(() -> {
                        final String clientId = OAuthParameters.createDefault(currentApiUrl, oAuthVersion).getClientId();
                        GuiHelper.runInEDT(() -> {
                            if (update == updates) {
                                this.setEnabled(!Utils.isEmpty(clientId));
                            }
                        });
                    });
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            final String currentApiUrl = apiUrl;
            final IOAuthParameters parameters = pnlAdvancedProperties.getAdvancedParameters();
            // the token is received by remote control, which is started for the time it takes if it is not running
            final boolean remoteControlIsRunning = Boolean.TRUE.equals(RemoteControl.PROP_REMOTECONTROL_ENABLED.get());
            if (!remoteControlIsRunning) {
                RemoteControl.start();
            }
            new OAuth20Authorization().authorize(parameters, token -> {
                if (!remoteControlIsRunning) {
                    RemoteControl.stop();
                }
                // a failed attempt, e.g. at a new token, leaves the token JOSM has as it is
                token.ifPresent(t -> {
                    OAuthAccessTokenHolder.getInstance().setAccessToken(currentApiUrl, t);
                    OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
                });
                GuiHelper.runInEDT(() -> {
                    if (!token.isPresent()) {
                        JOptionPane.showMessageDialog(OAuthAuthenticationPreferencesPanel.this,
                                tr("Authentication failed, please check browser for details."),
                                tr("OAuth Authentication Failed"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                    refreshView();
                });
            }, OsmScopes.getJosmScopes());
        }
    }

    /**
     * Remove the OAuth authorization token
     */
    private class RemoveAuthorisationAction extends AbstractAction {
        RemoveAuthorisationAction() {
            putValue(NAME, tr("Remove token"));
            putValue(SHORT_DESCRIPTION, tr("Remove token from JOSM. This does not revoke the token."));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OAuthAccessTokenHolder.getInstance().setAccessToken(apiUrl, null);
            OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
            refreshView();
        }
    }

    /**
     * Authorizes JOSM again, to generate a new Access Token
     */
    private class RenewAuthorisationAction extends AuthoriseNowAction {
        /**
         * Constructs a new {@code RenewAuthorisationAction}.
         */
        RenewAuthorisationAction() {
            putValue(NAME, tr("New Access Token"));
            putValue(SHORT_DESCRIPTION, tr("Click to step through the OAuth authorization process and generate a new Access Token"));
        }
    }

    /**
     * Runs a test whether we can access the OSM server with the current Access Token
     */
    private class TestAuthorisationAction extends AbstractAction {
        /**
         * Constructs a new {@code TestAuthorisationAction}.
         */
        TestAuthorisationAction() {
            putValue(NAME, tr("Test Access Token"));
            putValue(SHORT_DESCRIPTION, tr("Click test access to the OSM server with the current access token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            // a token pasted but not saved yet is what the user wants to know about, and none if the field was cleared
            final IOAuthToken token = isAccessTokenEdited() ? getEnteredAccessToken()
                    : OAuthAccessTokenHolder.getInstance().getAccessToken(apiUrl, oAuthVersion);
            if (token == null) {
                JOptionPane.showMessageDialog(OAuthAuthenticationPreferencesPanel.this,
                        tr("There is no Access Token to test."), tr("Test Access Token"), JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            TestAccessTokenTask task = new TestAccessTokenTask(
                    OAuthAuthenticationPreferencesPanel.this,
                    apiUrl,
                    token
            );
            MainApplication.worker.submit(task);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (AdvancedOAuthPropertiesPanel.PARAMETERS_CHANGED_PROP.equals(evt.getPropertyName())) {
            updateAuthoriseNowActions();

        } else if (OsmApiUrlInputPanel.API_URL_PROP.equals(evt.getPropertyName())) {
            setApiUrl((String) evt.getNewValue());
            refreshView();
        }
    }
}
