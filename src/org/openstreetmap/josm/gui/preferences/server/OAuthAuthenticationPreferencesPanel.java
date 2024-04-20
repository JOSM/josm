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
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.validation.routines.DomainValidator;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.oauth.AdvancedOAuthPropertiesPanel;
import org.openstreetmap.josm.gui.oauth.AuthorizationProcedure;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.oauth.TestAccessTokenTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * The preferences panel for the OAuth 1.0a preferences. This just a summary panel
 * showing the current Access Token Key and Access Token Secret, if the
 * user already has an Access Token.
 * <br>
 * For initial authorisation see {@link OAuthAuthorizationWizard}.
 * @since 2745
 */
public class OAuthAuthenticationPreferencesPanel extends JPanel implements PropertyChangeListener {
    private final JCheckBox cbUseForAllRequests = new JCheckBox();
    private final JCheckBox cbShowAdvancedParameters = new JCheckBox(tr("Display Advanced OAuth Parameters"));
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

        pnl.add(cbShowAdvancedParameters, GBC.eol().fill(GBC.HORIZONTAL));
        cbShowAdvancedParameters.setSelected(false);
        cbShowAdvancedParameters.addItemListener(
                evt -> pnlAdvancedProperties.setVisible(evt.getStateChange() == ItemEvent.SELECTED)
        );

        pnl.add(pnlAdvancedProperties, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 3, 0, 0));
        pnlAdvancedProperties.initialize(OsmApi.getOsmApi().getServerUrl());
        pnlAdvancedProperties.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );
        pnlAdvancedProperties.setVisible(false);
        return pnl;
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
     * Sets the URL of the OSM API for which this panel is currently displaying OAuth properties.
     *
     * @param apiUrl the api URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        pnlAdvancedProperties.setApiUrl(apiUrl);
        for (JPanel panel : Arrays.asList(this.pnlNotYetAuthorised, (JPanel) this.pnlAlreadyAuthorised.getComponent(6))) {
            for (Component component : panel.getComponents()) {
                if (component instanceof JButton && ((JButton) component).getAction() instanceof AuthoriseNowAction) {
                    ((AuthoriseNowAction) ((JButton) component).getAction()).updateEnabledState();
                }
            }
        }
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
            add(new JButton(new AuthoriseNowAction(AuthorizationProcedure.FULLY_AUTOMATIC)), GBC.eol());
            JButton authManually = new JButton(new AuthoriseNowAction(AuthorizationProcedure.MANUALLY));
            add(authManually, GBC.eol());
            ExpertToggleAction.addVisibilitySwitcher(authManually);

            // filler - grab remaining space
            add(new JPanel(), GBC.std().fill(GBC.BOTH));
        }
    }

    /**
     * The preferences panel displayed if there is currently an AccessToken available.
     *
     */
    private class AlreadyAuthorisedPanel extends JPanel {
        private final JosmTextField tfAccessTokenKey = new JosmTextField(null, null, 0, false);

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

            // -- access token key
            gc.gridy = 1;
            gc.gridx = 0;
            gc.gridwidth = 1;
            gc.weightx = 0.0;
            add(new JLabel(tr("Access Token Key:")), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(tfAccessTokenKey, gc);
            tfAccessTokenKey.setEditable(false);

            // -- access token secret
            gc.gridy = 2;
            gc.gridx = 0;
            gc.gridwidth = 1;
            gc.weightx = 0.0;
            add(new JLabel(tr("Access Token Secret:")), gc);

            // -- access token secret
            gc.gridy = 3;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(cbSaveToPreferences, gc);
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

            // -- action buttons
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
            btns.add(new JButton(new RenewAuthorisationAction(AuthorizationProcedure.FULLY_AUTOMATIC)));
            btns.add(new JButton(new TestAuthorisationAction()));
            btns.add(new JButton(new RemoveAuthorisationAction()));
            gc.gridy = 4;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(btns, gc);

            // filler - grab the remaining space
            gc.gridy = 6;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }

        protected final void refreshView() {
            switch (oAuthVersion) {
                case OAuth20:
                case OAuth21:
                    String token = "";
                    if (apiUrl != null) {
                        OAuth20Token bearerToken = (OAuth20Token) OAuthAccessTokenHolder.getInstance().getAccessToken(apiUrl, oAuthVersion);
                        token = bearerToken == null ? "" : bearerToken.getBearerToken();
                    }
                    tfAccessTokenKey.setText(token == null ? "" : token);
                    break;
                default:
            }
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());
        }
    }

    /**
     * Action to authorise the current user
     */
    private class AuthoriseNowAction extends AbstractAction {
        private final AuthorizationProcedure procedure;

        AuthoriseNowAction(AuthorizationProcedure procedure) {
            this.procedure = procedure;
            putValue(NAME, tr("{0} ({1})", tr("Authorize now"), procedure.getText()));
            putValue(SHORT_DESCRIPTION, procedure.getDescription());
            if (procedure == AuthorizationProcedure.FULLY_AUTOMATIC) {
                new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            }
            updateEnabledState();
        }

        void updateEnabledState() {
            if (procedure == AuthorizationProcedure.MANUALLY) {
                this.setEnabled(true);
            } else if (Utils.isValidUrl(apiUrl)) {
                final URI apiURI;
                try {
                    apiURI = new URI(apiUrl);
                } catch (URISyntaxException e) {
                    Logging.trace(e);
                    return;
                }
                if (DomainValidator.getInstance().isValid(apiURI.getHost())) {
                    // We want to avoid trying to make connection with an invalid URL
                    final String currentApiUrl = apiUrl;
                    MainApplication.worker.execute(() -> {
                        final String clientId = OAuthParameters.createFromApiUrl(apiUrl, oAuthVersion).getClientId();
                        if (Objects.equals(apiUrl, currentApiUrl)) {
                            GuiHelper.runInEDT(() -> this.setEnabled(!Utils.isEmpty(clientId)));
                        }
                    });
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            OAuthAuthorizationWizard wizard = new OAuthAuthorizationWizard(
                    OAuthAuthenticationPreferencesPanel.this,
                    procedure,
                    apiUrl,
                    MainApplication.worker,
                    oAuthVersion,
                    pnlAdvancedProperties.getAdvancedParameters()
                    );
            try {
                wizard.showDialog(token -> GuiHelper.runInEDT(OAuthAuthenticationPreferencesPanel.this::refreshView));
            } catch (UserCancelException userCancelException) {
                Logging.trace(userCancelException);
                return;
            }
            pnlAdvancedProperties.setAdvancedParameters(wizard.getOAuthParameters());
            refreshView();
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
     * Launches the OAuthAuthorisationWizard to generate a new Access Token
     */
    private class RenewAuthorisationAction extends AuthoriseNowAction {
        /**
         * Constructs a new {@code RenewAuthorisationAction}.
         */
        RenewAuthorisationAction(AuthorizationProcedure procedure) {
            super(procedure);
            putValue(NAME, tr("New Access Token"));
            putValue(SHORT_DESCRIPTION, tr("Click to step through the OAuth authorization process and generate a new Access Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
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
            IOAuthToken token = OAuthAccessTokenHolder.getInstance().getAccessToken(apiUrl, OAuthVersion.OAuth20);
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
        if (!evt.getPropertyName().equals(OsmApiUrlInputPanel.API_URL_PROP))
            return;
        setApiUrl((String) evt.getNewValue());
    }
}
