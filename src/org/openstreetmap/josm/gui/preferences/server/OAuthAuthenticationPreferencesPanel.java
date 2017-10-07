// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.oauth.AdvancedOAuthPropertiesPanel;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.oauth.TestAccessTokenTask;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * The preferences panel for the OAuth preferences. This just a summary panel
 * showing the current Access Token Key and Access Token Secret, if the
 * user already has an Access Token.
 *
 * For initial authorisation see {@link OAuthAuthorizationWizard}.
 * @since 2745
 */
public class OAuthAuthenticationPreferencesPanel extends JPanel implements PropertyChangeListener {
    private final JCheckBox cbShowAdvancedParameters = new JCheckBox();
    private final JCheckBox cbSaveToPreferences = new JCheckBox(tr("Save to preferences"));
    private final JPanel pnlAuthorisationMessage = new JPanel(new BorderLayout());
    private final NotYetAuthorisedPanel pnlNotYetAuthorised = new NotYetAuthorisedPanel();
    private final AdvancedOAuthPropertiesPanel pnlAdvancedProperties = new AdvancedOAuthPropertiesPanel();
    private final AlreadyAuthorisedPanel pnlAlreadyAuthorised = new AlreadyAuthorisedPanel();
    private String apiUrl;

    /**
     * Create the panel
     */
    public OAuthAuthenticationPreferencesPanel() {
        build();
        refreshView();
    }

    /**
     * Builds the panel for entering the advanced OAuth parameters
     *
     * @return panel with advanced settings
     */
    protected JPanel buildAdvancedPropertiesPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(cbShowAdvancedParameters, gc);
        cbShowAdvancedParameters.setSelected(false);
        cbShowAdvancedParameters.addItemListener(
                evt -> pnlAdvancedProperties.setVisible(evt.getStateChange() == ItemEvent.SELECTED)
        );

        gc.gridx = 1;
        gc.weightx = 1.0;
        JMultilineLabel lbl = new JMultilineLabel(tr("Display Advanced OAuth Parameters"));
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
        pnl.add(lbl, gc);

        gc.gridy = 1;
        gc.gridx = 1;
        gc.insets = new Insets(3, 0, 3, 0);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(pnlAdvancedProperties, gc);
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
        GridBagConstraints gc = new GridBagConstraints();

        // the panel for the OAuth parameters. pnlAuthorisationMessage is an
        // empty panel. It is going to be filled later, depending on the
        // current OAuth state in JOSM.
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weighty = 1.0;
        gc.weightx = 1.0;
        gc.insets = new Insets(10, 0, 0, 0);
        add(pnlAuthorisationMessage, gc);
    }

    protected void refreshView() {
        pnlAuthorisationMessage.removeAll();
        if (OAuthAccessTokenHolder.getInstance().containsAccessToken()) {
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
    }

    /**
     * Initializes the panel from preferences
     */
    public void initFromPreferences() {
        setApiUrl(OsmApi.getOsmApi().getServerUrl().trim());
        refreshView();
    }

    /**
     * Saves the current values to preferences
     */
    public void saveToPreferences() {
        OAuthAccessTokenHolder.getInstance().setSaveToPreferences(cbSaveToPreferences.isSelected());
        OAuthAccessTokenHolder.getInstance().save(CredentialsManager.getInstance());
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
            GridBagConstraints gc = new GridBagConstraints();

            // A message explaining that the user isn't authorised yet
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.insets = new Insets(0, 0, 3, 0);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            JMultilineLabel lbl = new JMultilineLabel(
                    tr("You do not have an Access Token yet to access the OSM server using OAuth. Please authorize first."));
            add(lbl, gc);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));

            // Action for authorising now
            gc.gridy = 1;
            gc.fill = GridBagConstraints.NONE;
            gc.weightx = 0.0;
            add(new JButton(new AuthoriseNowAction()), gc);

            // filler - grab remaining space
            gc.gridy = 2;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }
    }

    /**
     * The preferences panel displayed if there is currently an AccessToken available.
     *
     */
    private class AlreadyAuthorisedPanel extends JPanel {
        private final JosmTextField tfAccessTokenKey = new JosmTextField();
        private final JosmTextField tfAccessTokenSecret = new JosmTextField();

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

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(tfAccessTokenSecret, gc);
            tfAccessTokenSecret.setEditable(false);

            // -- access token secret
            gc.gridy = 3;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(cbSaveToPreferences, gc);
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

            // -- action buttons
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
            btns.add(new JButton(new RenewAuthorisationAction()));
            btns.add(new JButton(new TestAuthorisationAction()));
            gc.gridy = 4;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(btns, gc);

            // the panel with the advanced options
            gc.gridy = 5;
            gc.gridx = 0;
            gc.gridwidth = 2;
            gc.weightx = 1.0;
            add(buildAdvancedPropertiesPanel(), gc);

            // filler - grab the remaining space
            gc.gridy = 6;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            add(new JPanel(), gc);
        }

        protected final void refreshView() {
            String v = OAuthAccessTokenHolder.getInstance().getAccessTokenKey();
            tfAccessTokenKey.setText(v == null ? "" : v);
            v = OAuthAccessTokenHolder.getInstance().getAccessTokenSecret();
            tfAccessTokenSecret.setText(v == null ? "" : v);
            cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());
        }
    }

    /**
     * Action to authorise the current user
     */
    private class AuthoriseNowAction extends AbstractAction {
        AuthoriseNowAction() {
            putValue(NAME, tr("Authorize now"));
            putValue(SHORT_DESCRIPTION, tr("Click to step through the OAuth authorization process"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            OAuthAuthorizationWizard wizard = new OAuthAuthorizationWizard(
                    OAuthAuthenticationPreferencesPanel.this,
                    apiUrl,
                    MainApplication.worker);
            try {
                wizard.showDialog();
            } catch (UserCancelException ignore) {
                Logging.trace(ignore);
                return;
            }
            pnlAdvancedProperties.setAdvancedParameters(wizard.getOAuthParameters());
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
        RenewAuthorisationAction() {
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
            OAuthToken token = OAuthAccessTokenHolder.getInstance().getAccessToken();
            OAuthParameters parameters = OAuthParameters.createFromApiUrl(OsmApi.getOsmApi().getServerUrl());
            TestAccessTokenTask task = new TestAccessTokenTask(
                    OAuthAuthenticationPreferencesPanel.this,
                    apiUrl,
                    parameters,
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
