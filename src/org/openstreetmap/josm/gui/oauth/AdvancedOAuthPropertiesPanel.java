// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.data.oauth.osm.OsmScopes;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Panel allowing the user to setup advanced OAuth parameters:
 * <ul>
 * <li>Client ID</li>
 * <li>Client secret</li>
 * <li>Redirect URI</li>
 * <li>Access token URL</li>
 * <li>Authorize URL</li>
 * <li>Access token, which can be pasted instead of authorizing JOSM</li>
 * </ul>
 *
 * @see OAuthParameters
 * @since 2746
 */
public class AdvancedOAuthPropertiesPanel extends VerticallyScrollablePanel {
    /**
     * Property fired whenever the parameters this panel would return change: the client id was edited,
     * or "use default settings" was toggled. Listeners re-read {@link #getAdvancedParameters()}.
     * @since 19627
     */
    public static final String PARAMETERS_CHANGED_PROP = AdvancedOAuthPropertiesPanel.class.getName() + ".parametersChanged";

    private final JCheckBox cbUseDefaults = new JCheckBox(tr("Use default settings"));
    private final JosmTextField tfClientId = new JosmTextField();
    private final JosmTextField tfClientSecret = new JosmTextField();
    private final JosmTextField tfRedirectUri = new JosmTextField();
    private final JosmTextField tfAccessTokenURL = new JosmTextField();
    private final JosmTextField tfAuthoriseURL = new JosmTextField();
    private final JLabel lblAccessToken = new JLabel(tr("Access Token:"));
    private final JosmTextField tfAccessToken = new JosmTextField();
    private final OAuthVersion oauthVersion;
    private transient UseDefaultItemListener ilUseDefault;
    private String apiUrl;

    /**
     * Constructs a new {@code AdvancedOAuthPropertiesPanel}.
     * @param oauthVersion The OAuth version to make the panel for
     */
    public AdvancedOAuthPropertiesPanel(OAuthVersion oauthVersion) {
        this.oauthVersion = oauthVersion;
        build();
    }

    protected final void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 3, 3);
        gc.gridwidth = 3;
        add(cbUseDefaults, gc);

        // -- client id
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        add(label(tr("Client ID:"), tfClientId), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfClientId, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfClientId);

        // -- client secret
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(label(tr("Client Secret:"), tfClientSecret), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfClientSecret, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfClientSecret);

        // -- redirect URI
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(label(tr("Redirect URL:"), tfRedirectUri), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfRedirectUri, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfRedirectUri);

        // -- access token URL
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(label(tr("Access Token URL:"), tfAccessTokenURL), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenURL);

        // -- authorise URL
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(label(tr("Authorize URL:"), tfAuthoriseURL), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAuthoriseURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAuthoriseURL);

        // -- access token: not one of the settings, so it can be pasted whether or not they are the default ones
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        lblAccessToken.setLabelFor(tfAccessToken);
        add(lblAccessToken, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessToken, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessToken);

        setToolTips();

        ilUseDefault = new UseDefaultItemListener();
        cbUseDefaults.addItemListener(ilUseDefault);
        cbUseDefaults.setSelected(true);
        setChildComponentsVisible(false);
        cbUseDefaults.addItemListener(e -> fireParametersChanged());
        tfClientId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireParametersChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireParametersChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireParametersChanged();
            }
        });
    }

    /**
     * Creates the label of a field, which has the tool tip of the field.
     * @param text the text of the label
     * @param field the field
     * @return the label
     */
    private static JLabel label(String text, JosmTextField field) {
        final JLabel label = new JLabel(text);
        label.setLabelFor(field);
        return label;
    }

    /**
     * Explains each field, for the fields and their labels.
     */
    private void setToolTips() {
        final String scopes = Stream.of(OsmScopes.getJosmScopes()).map(Enum::name).collect(Collectors.joining(", "));
        cbUseDefaults.setToolTipText(tr("<html>Use the OAuth settings JOSM has built in for this server, or which the server publishes.<br>"
                + "Untick to enter the settings of an OAuth 2.0 application you registered on the server yourself.</html>"));
        tfClientId.setToolTipText(tr("<html>The Client ID of an OAuth 2.0 application registered on this server.<br>"
                + "On an openstreetmap-website server, register one under My Settings &gt; OAuth 2 applications,<br>"
                + "with the redirect URL below and the permissions {0}.</html>", scopes));
        tfClientSecret.setToolTipText(tr("<html>Only needed if the application is registered as confidential.<br>"
                + "The applications JOSM has built in are public clients and have none.</html>"));
        tfRedirectUri.setToolTipText(tr("<html>Must be exactly the redirect URL registered with the application.<br>"
                + "JOSM receives the authorization at {0}, by remote control.</html>", "http://127.0.0.1:8111/oauth_authorization"));
        tfAccessTokenURL.setToolTipText(tr("<html>Where JOSM gets the Access Token from.<br>"
                + "Taken from the OAuth metadata of the server if it publishes any; on an openstreetmap-website server,<br>"
                + "this is the address of the website followed by {0}, not the API URL.</html>", "/oauth2/token"));
        tfAuthoriseURL.setToolTipText(tr("<html>Where the web browser is sent to authorize JOSM.<br>"
                + "Taken from the OAuth metadata of the server if it publishes any; on an openstreetmap-website server,<br>"
                + "this is the address of the website followed by {0}, not the API URL.</html>", "/oauth2/authorize"));
        tfAccessToken.setToolTipText(tr("<html>The Access Token JOSM uses for this server.<br>"
                + "A token issued outside JOSM can be pasted here instead of authorizing JOSM; it needs the permissions {0}.</html>",
                scopes));
        for (Component c : getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getLabelFor() instanceof JComponent) {
                ((JLabel) c).setToolTipText(((JComponent) ((JLabel) c).getLabelFor()).getToolTipText());
            }
        }
    }

    private void fireParametersChanged() {
        firePropertyChange(PARAMETERS_CHANGED_PROP, null, null);
    }

    /**
     * Get the access token as entered in the panel.
     * @return the access token, without surrounding white space; empty if there is none
     * @since 19707
     */
    public String getAccessToken() {
        return tfAccessToken.getText().trim();
    }

    /**
     * Sets the access token to be displayed.
     * @param accessToken the access token, or {@code null} if there is none
     * @since 19707
     */
    public void setAccessToken(String accessToken) {
        tfAccessToken.setText(accessToken);
    }

    /**
     * Determines if the panel is set to use the default parameters for the API URL rather than
     * the values entered in its fields.
     * @return {@code true} if "use default settings" is selected
     * @since 19627
     */
    public boolean isUseDefaultSettings() {
        return cbUseDefaults.isSelected();
    }

    /**
     * Get the client id as entered in the panel, without falling back to the defaults.
     * @return the client id field's text, without surrounding white space
     * @since 19627
     */
    public String getEnteredClientId() {
        return tfClientId.getText().trim();
    }

    protected boolean hasCustomSettings() {
        IOAuthParameters params = OAuthParameters.createDefault(apiUrl, this.oauthVersion);
        return !params.equals(getAdvancedParameters());
    }

    protected boolean confirmOverwriteCustomSettings() {
        ButtonSpec[] buttons = {
                new ButtonSpec(
                        tr("Continue"),
                        new ImageProvider("ok"),
                        tr("Click to reset the OAuth settings to default values"),
                        null /* no dedicated help topic */
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        new ImageProvider("cancel"),
                        tr("Click to abort resetting to the OAuth default values"),
                        null /* no dedicated help topic */
                )
        };
        return 0 == HelpAwareOptionPane.showOptionDialog(
                this,
                tr(
                        "<html>JOSM is about to reset the OAuth settings to default values.<br>"
                        + "The current custom settings are not saved.</html>"
                ),
                tr("Overwrite custom OAuth settings?"),
                JOptionPane.WARNING_MESSAGE,
                null, /* no dedicated icon */
                buttons,
                buttons[0],
                HelpUtil.ht("/Dialog/OAuthAuthorisationWizard")
        );
    }

    protected void resetToDefaultSettings() {
        IOAuthParameters iParams = OAuthParameters.createDefault(apiUrl, this.oauthVersion);
        GuiHelper.runInEDTAndWaitWithException(() -> {
                    cbUseDefaults.setSelected(true);
                    switch (this.oauthVersion) {
                        case OAuth20:
                        case OAuth21:
                            OAuth20Parameters params20 = (OAuth20Parameters) iParams;
                            tfClientId.setText(params20.getClientId());
                            tfClientSecret.setText(params20.getClientSecret());
                            tfAccessTokenURL.setText(params20.getAccessTokenUrl());
                            tfAuthoriseURL.setText(params20.getAuthorizationUrl());
                            tfRedirectUri.setText(params20.getRedirectUri());
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported OAuth version: " + this.oauthVersion);
                    }
                    setChildComponentsVisible(false);
                });
    }

    /**
     * Shows or hides the OAuth parameters: they are shown when the user enters them, that is when the default
     * settings are not used. The access token is not one of them, and is always shown.
     * @param visible {@code true} to show the parameters
     * @since 19707
     */
    protected void setChildComponentsVisible(boolean visible) {
        for (Component c: getComponents()) {
            if (c == tfAccessToken || c == lblAccessToken) {
                continue;
            }
            if (c instanceof JosmTextField || c instanceof JLabel) {
                c.setVisible(visible);
            }
        }
        revalidate();
    }

    /**
     * Replies the OAuth parameters currently edited in this properties panel.
     *
     * @return the OAuth parameters
     */
    public IOAuthParameters getAdvancedParameters() {
        if (cbUseDefaults.isSelected())
            return OAuthParameters.createDefault(apiUrl, this.oauthVersion);
        return getDisplayedParameters();
    }

    /**
     * Replies the OAuth parameters as currently displayed in this properties panel: the default ones are shown when
     * "use default settings" is ticked, as far as they are known yet. Unlike {@link #getAdvancedParameters()}, this
     * never makes a network request, and some of the parameters may be empty. White space around the values, which
     * is easily copied along with them, is left out: a client secret with a trailing space is refused by the server.
     *
     * @return the OAuth parameters
     * @since 19707
     */
    public OAuth20Parameters getDisplayedParameters() {
        return new OAuth20Parameters(
                tfClientId.getText().trim(),
                tfClientSecret.getText().trim(),
                tfAccessTokenURL.getText().trim(),
                tfAuthoriseURL.getText().trim(),
                apiUrl,
                tfRedirectUri.getText().trim()
                );
    }

    /**
     * Sets the advanced parameters to be displayed
     *
     * @param parameters the advanced parameters. Must not be null.
     * @throws IllegalArgumentException if parameters is null.
     */
    public void setAdvancedParameters(IOAuthParameters parameters) {
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        if (parameters.equals(OAuthParameters.createDefault(apiUrl, parameters.getOAuthVersion()))) {
            cbUseDefaults.setSelected(true);
            setChildComponentsVisible(false);
        } else {
            cbUseDefaults.setSelected(false);
            setChildComponentsVisible(true);
            if (parameters instanceof OAuth20Parameters) {
                OAuth20Parameters parameters20 = (OAuth20Parameters) parameters;
                tfClientId.setText(parameters20.getClientId());
                tfClientSecret.setText(parameters20.getClientSecret());
                tfAccessTokenURL.setText(parameters20.getAccessTokenUrl());
                tfAuthoriseURL.setText(parameters20.getAuthorizationUrl());
                tfRedirectUri.setText(parameters20.getRedirectUri());
            }
        }
    }

    /**
     * Initializes the panel from the values in the preferences <code>preferences</code>.
     *
     * @param paramApiUrl the API URL. Must not be null.
     * @throws IllegalArgumentException if paramApiUrl is null
     */
    public void initialize(String paramApiUrl) {
        CheckParameterUtil.ensureParameterNotNull(paramApiUrl, "paramApiUrl");
        this.apiUrl = paramApiUrl;
        // "use default settings" is a setting of each server
        boolean useDefault = OAuthParameters.isUseDefault(paramApiUrl);
        ilUseDefault.setEnabled(false);
        cbUseDefaults.setSelected(useDefault);
        setChildComponentsVisible(!useDefault);
        MainApplication.worker.execute(() -> {
            if (useDefault) {
                this.resetToDefaultSettings();
            } else {
                // createFromApiUrl may make a network request
                final IOAuthParameters parameters = OAuthParameters.createFromApiUrl(paramApiUrl, this.oauthVersion);
                GuiHelper.runInEDT(() -> setAdvancedParameters(parameters));
            }
            GuiHelper.runInEDT(() -> ilUseDefault.setEnabled(true));
        });
    }

    /**
     * Remembers the current values in the preferences <code>pref</code>: whether the server uses the default
     * settings, and the settings entered if not. Settings entered earlier are kept when the default settings are
     * used, so that they are there again when they are needed again.
     */
    public void rememberPreferences() {
        if (apiUrl == null) {
            return; // not initialized
        }
        OAuthParameters.setUseDefault(apiUrl, cbUseDefaults.isSelected());
        if (!cbUseDefaults.isSelected()) {
            getAdvancedParameters().rememberPreferences();
        }
    }

    class UseDefaultItemListener implements ItemListener {
        private boolean enabled;

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (!enabled) return;
            switch (e.getStateChange()) {
            case ItemEvent.SELECTED:
                if (hasCustomSettings() && !confirmOverwriteCustomSettings()) {
                    cbUseDefaults.setSelected(false);
                    return;
                }
                MainApplication.worker.execute(AdvancedOAuthPropertiesPanel.this::resetToDefaultSettings);
                break;
            case ItemEvent.DESELECTED:
                setChildComponentsVisible(true);
                break;
            default: // Do nothing
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Sets the URL of the OSM API for which this panel is currently displaying OAuth properties.
     *
     * @param apiUrl the api URL
     * @since 5422
     */
    public void setApiUrl(String apiUrl) {
        if (this.apiUrl != null && apiUrl != null
                && !Objects.equals(OAuthParameters.getHost(this.apiUrl), OAuthParameters.getHost(apiUrl))) {
            // another server, with settings of its own
            initialize(apiUrl);
            return;
        }
        this.apiUrl = apiUrl;
        if (cbUseDefaults.isSelected()) {
            MainApplication.worker.execute(this::resetToDefaultSettings);
        }
    }
}
