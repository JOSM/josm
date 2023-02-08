// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Panel allowing the user to setup advanced OAuth parameters:
 * <ul>
 * <li>Consumer key</li>
 * <li>Consumer secret</li>
 * <li>Request token URL</li>
 * <li>Access token URL</li>
 * <li>Authorize URL</li>
 * <li>OSM login URL</li>
 * <li>OSM logout URL</li>
 * </ul>
 *
 * @see OAuthParameters
 * @since 2746
 */
public class AdvancedOAuthPropertiesPanel extends VerticallyScrollablePanel {

    private final JCheckBox cbUseDefaults = new JCheckBox(tr("Use default settings"));
    private final JosmTextField tfConsumerKey = new JosmTextField();
    private final JosmTextField tfConsumerSecret = new JosmTextField();
    private final JosmTextField tfRequestTokenURL = new JosmTextField();
    private final JosmTextField tfAccessTokenURL = new JosmTextField();
    private final JosmTextField tfAuthoriseURL = new JosmTextField();
    private final JosmTextField tfOsmLoginURL = new JosmTextField();
    private final JosmTextField tfOsmLogoutURL = new JosmTextField();
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

        // -- consumer key
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        if (this.oauthVersion == OAuthVersion.OAuth10a) {
            add(new JLabel(tr("Consumer Key:")), gc);
        } else {
            add(new JLabel(tr("Client ID:")), gc);
        }

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerKey, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerKey);

        // -- consumer secret
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        if (this.oauthVersion == OAuthVersion.OAuth10a) {
            add(new JLabel(tr("Consumer Secret:")), gc);
        } else {
            add(new JLabel(tr("Client Secret:")), gc);
        }

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerSecret, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerSecret);

        // -- request token URL
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        if (this.oauthVersion == OAuthVersion.OAuth10a) {
            add(new JLabel(tr("Request Token URL:")), gc);
        } else {
            add(new JLabel(tr("Redirect URL:")), gc);
        }

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfRequestTokenURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfRequestTokenURL);

        // -- access token URL
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Access Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenURL);

        // -- authorise URL
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Authorize URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAuthoriseURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAuthoriseURL);

        if (this.oauthVersion == OAuthVersion.OAuth10a) {
            // -- OSM login URL
            gc.gridy++;
            gc.gridx = 0;
            gc.weightx = 0.0;
            add(new JLabel(tr("OSM login URL:")), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(tfOsmLoginURL, gc);
            SelectAllOnFocusGainedDecorator.decorate(tfOsmLoginURL);

            // -- OSM logout URL
            gc.gridy++;
            gc.gridx = 0;
            gc.weightx = 0.0;
            add(new JLabel(tr("OSM logout URL:")), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(tfOsmLogoutURL, gc);
            SelectAllOnFocusGainedDecorator.decorate(tfOsmLogoutURL);
        }

        ilUseDefault = new UseDefaultItemListener();
        cbUseDefaults.addItemListener(ilUseDefault);
    }

    protected boolean hasCustomSettings() {
        OAuthParameters params = OAuthParameters.createDefault(apiUrl);
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
        cbUseDefaults.setSelected(true);
        IOAuthParameters iParams = OAuthParameters.createDefault(apiUrl, this.oauthVersion);
        switch (this.oauthVersion) {
            case OAuth10a:
                OAuthParameters params = (OAuthParameters) iParams;
                tfConsumerKey.setText(params.getConsumerKey());
                tfConsumerSecret.setText(params.getConsumerSecret());
                tfRequestTokenURL.setText(params.getRequestTokenUrl());
                tfAccessTokenURL.setText(params.getAccessTokenUrl());
                tfAuthoriseURL.setText(params.getAuthoriseUrl());
                tfOsmLoginURL.setText(params.getOsmLoginUrl());
                tfOsmLogoutURL.setText(params.getOsmLogoutUrl());
                break;
            case OAuth20:
            case OAuth21:
                OAuth20Parameters params20 = (OAuth20Parameters) iParams;
                tfConsumerKey.setText(params20.getClientId());
                tfConsumerSecret.setText(params20.getClientSecret());
                tfAccessTokenURL.setText(params20.getAccessTokenUrl());
                tfAuthoriseURL.setText(params20.getAuthorizationUrl());
                tfRequestTokenURL.setText(params20.getRedirectUri());
        }

        setChildComponentsEnabled(false);
    }

    protected void setChildComponentsEnabled(boolean enabled) {
        for (Component c: getComponents()) {
            if (c instanceof JosmTextField || c instanceof JLabel) {
                c.setEnabled(enabled);
            }
        }
    }

    /**
     * Replies the OAuth parameters currently edited in this properties panel.
     *
     * @return the OAuth parameters
     */
    public IOAuthParameters getAdvancedParameters() {
        if (cbUseDefaults.isSelected())
            return OAuthParameters.createDefault(apiUrl, this.oauthVersion);
        if (this.oauthVersion == OAuthVersion.OAuth10a) {
            return new OAuthParameters(
                    tfConsumerKey.getText(),
                    tfConsumerSecret.getText(),
                    tfRequestTokenURL.getText(),
                    tfAccessTokenURL.getText(),
                    tfAuthoriseURL.getText(),
                    tfOsmLoginURL.getText(),
                    tfOsmLogoutURL.getText());
        }
        return new OAuth20Parameters(
                tfConsumerKey.getText(),
                tfConsumerSecret.getText(),
                tfAuthoriseURL.getText(),
                tfAccessTokenURL.getText(),
                tfRequestTokenURL.getText()
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
            setChildComponentsEnabled(false);
        } else {
            cbUseDefaults.setSelected(false);
            setChildComponentsEnabled(true);
            if (parameters instanceof OAuthParameters) {
                OAuthParameters parameters10 = (OAuthParameters) parameters;
                tfConsumerKey.setText(parameters10.getConsumerKey() == null ? "" : parameters10.getConsumerKey());
                tfConsumerSecret.setText(parameters10.getConsumerSecret() == null ? "" : parameters10.getConsumerSecret());
                tfRequestTokenURL.setText(parameters10.getRequestTokenUrl() == null ? "" : parameters10.getRequestTokenUrl());
                tfAccessTokenURL.setText(parameters10.getAccessTokenUrl() == null ? "" : parameters10.getAccessTokenUrl());
                tfAuthoriseURL.setText(parameters10.getAuthoriseUrl() == null ? "" : parameters10.getAuthoriseUrl());
                tfOsmLoginURL.setText(parameters10.getOsmLoginUrl() == null ? "" : parameters10.getOsmLoginUrl());
                tfOsmLogoutURL.setText(parameters10.getOsmLogoutUrl() == null ? "" : parameters10.getOsmLogoutUrl());
            } else if (parameters instanceof OAuth20Parameters) {
                OAuth20Parameters parameters20 = (OAuth20Parameters) parameters;
                tfConsumerKey.setText(parameters20.getClientId());
                tfConsumerSecret.setText(parameters20.getClientSecret());
                tfAccessTokenURL.setText(parameters20.getAccessTokenUrl());
                tfAuthoriseURL.setText(parameters20.getAuthorizationUrl());
                tfRequestTokenURL.setText(parameters20.getRedirectUri());
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
        setApiUrl(paramApiUrl);
        boolean useDefault = Config.getPref().getBoolean("oauth.settings.use-default", true);
        ilUseDefault.setEnabled(false);
        if (useDefault) {
            resetToDefaultSettings();
        } else {
            setAdvancedParameters(OAuthParameters.createFromApiUrl(paramApiUrl));
        }
        ilUseDefault.setEnabled(true);
    }

    /**
     * Remembers the current values in the preferences <code>pref</code>.
     */
    public void rememberPreferences() {
        Config.getPref().putBoolean("oauth.settings.use-default", cbUseDefaults.isSelected());
        if (cbUseDefaults.isSelected()) {
            new OAuthParameters(null, null, null, null, null, null, null).rememberPreferences();
        } else {
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
                resetToDefaultSettings();
                break;
            case ItemEvent.DESELECTED:
                setChildComponentsEnabled(true);
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
        this.apiUrl = apiUrl;
        if (cbUseDefaults.isSelected()) {
            resetToDefaultSettings();
        }
    }
}
