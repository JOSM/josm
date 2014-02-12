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

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
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
 * </ul>
 *
 * @see OAuthParameters
 * @since 2746
 */
public class AdvancedOAuthPropertiesPanel extends VerticallyScrollablePanel {

    private JCheckBox cbUseDefaults;
    private JosmTextField tfConsumerKey;
    private JosmTextField tfConsumerSecret;
    private JosmTextField tfRequestTokenURL;
    private JosmTextField tfAccessTokenURL;
    private JosmTextField tfAuthoriseURL;
    private UseDefaultItemListener ilUseDefault;
    private String apiUrl;

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0, 3, 3);
        gc.gridwidth = 2;
        cbUseDefaults = new JCheckBox(tr("Use default settings"));
        add(cbUseDefaults, gc);

        // -- consumer key
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        add(new JLabel(tr("Consumer Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerKey = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerKey);

        // -- consumer secret
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Consumer Secret:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerSecret = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerSecret);

        // -- request token URL
        gc.gridy = 3;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Request Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfRequestTokenURL = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfRequestTokenURL);

        // -- access token URL
        gc.gridy = 4;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Access Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenURL = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenURL);


        // -- authorise URL
        gc.gridy = 5;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Authorize URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAuthoriseURL = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAuthoriseURL);

        cbUseDefaults.addItemListener(ilUseDefault = new UseDefaultItemListener());
    }

    protected boolean hasCustomSettings() {
        OAuthParameters params = OAuthParameters.createDefault(apiUrl);
        return
           ! tfConsumerKey.getText().equals(params.getConsumerKey())
        || ! tfConsumerSecret.getText().equals(params.getConsumerSecret())
        || ! tfRequestTokenURL.getText().equals(params.getRequestTokenUrl())
        || ! tfAccessTokenURL.getText().equals(params.getAccessTokenUrl())
        || ! tfAuthoriseURL.getText().equals(params.getAuthoriseUrl());
    }

    protected boolean confirmOverwriteCustomSettings() {
        ButtonSpec[] buttons = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Continue"),
                        ImageProvider.get("ok"),
                        tr("Click to reset the OAuth settings to default values"),
                        null /* no dedicated help topic */
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to abort resetting to the OAuth default values"),
                        null /* no dedicated help topic */
                )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                AdvancedOAuthPropertiesPanel.this,
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

        return ret == 0; // OK button clicked
    }

    protected void resetToDefaultSettings() {
        cbUseDefaults.setSelected(true);
        OAuthParameters params = OAuthParameters.createDefault(apiUrl);
        tfConsumerKey.setText(params.getConsumerKey());
        tfConsumerSecret.setText(params.getConsumerSecret());
        tfRequestTokenURL.setText(params.getRequestTokenUrl());
        tfAccessTokenURL.setText(params.getAccessTokenUrl());
        tfAuthoriseURL.setText(params.getAuthoriseUrl());

        setChildComponentsEnabled(false);
    }

    protected void setChildComponentsEnabled(boolean enabled){
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
    public OAuthParameters getAdvancedParameters() {
        if (cbUseDefaults.isSelected())
            return OAuthParameters.createDefault(apiUrl);
        OAuthParameters parameters = new OAuthParameters();
        parameters.setConsumerKey(tfConsumerKey.getText());
        parameters.setConsumerSecret(tfConsumerSecret.getText());
        parameters.setRequestTokenUrl(tfRequestTokenURL.getText());
        parameters.setAccessTokenUrl(tfAccessTokenURL.getText());
        parameters.setAuthoriseUrl(tfAuthoriseURL.getText());
        return parameters;
    }

    /**
     * Sets the advanced parameters to be displayed
     *
     * @param parameters the advanced parameters. Must not be null.
     * @throws IllegalArgumentException thrown if parameters is null.
     */
    public void setAdvancedParameters(OAuthParameters parameters) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        if (parameters.equals(OAuthParameters.createDefault(apiUrl))) {
            cbUseDefaults.setSelected(true);
            setChildComponentsEnabled(false);
        } else {
            cbUseDefaults.setSelected(false);
            setChildComponentsEnabled(true);
            tfConsumerKey.setText( parameters.getConsumerKey() == null ? "" : parameters.getConsumerKey());
            tfConsumerSecret.setText( parameters.getConsumerSecret() == null ? "" : parameters.getConsumerSecret());
            tfRequestTokenURL.setText(parameters.getRequestTokenUrl() == null ? "" : parameters.getRequestTokenUrl());
            tfAccessTokenURL.setText(parameters.getAccessTokenUrl() == null ? "" : parameters.getAccessTokenUrl());
            tfAuthoriseURL.setText(parameters.getAuthoriseUrl() == null ? "" : parameters.getAuthoriseUrl());
        }
    }

    /**
     * Constructs a new {@code AdvancedOAuthPropertiesPanel}.
     */
    public AdvancedOAuthPropertiesPanel() {
        build();
    }

    /**
     * Initializes the panel from the values in the preferences <code>preferences</code>.
     *
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException thrown if pref is null
     */
    public void initFromPreferences(Preferences pref) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        setApiUrl(pref.get("osm-server.url"));
        boolean useDefault = pref.getBoolean("oauth.settings.use-default", true);
        ilUseDefault.setEnabled(false);
        if (useDefault) {
            resetToDefaultSettings();
        } else {
            cbUseDefaults.setSelected(false);
            tfConsumerKey.setText(pref.get("oauth.settings.consumer-key", OAuthParameters.DEFAULT_JOSM_CONSUMER_KEY));
            tfConsumerSecret.setText(pref.get("oauth.settings.consumer-secret", OAuthParameters.DEFAULT_JOSM_CONSUMER_SECRET));
            tfRequestTokenURL.setText(pref.get("oauth.settings.request-token-url", OAuthParameters.DEFAULT_REQUEST_TOKEN_URL));
            tfAccessTokenURL.setText(pref.get("oauth.settings.access-token-url", OAuthParameters.DEFAULT_ACCESS_TOKEN_URL));
            tfAuthoriseURL.setText(pref.get("oauth.settings.authorise-url", OAuthParameters.DEFAULT_AUTHORISE_URL));
            setChildComponentsEnabled(true);
        }
        ilUseDefault.setEnabled(true);
    }

    /**
     * Remembers the current values in the preferences <code>pref</code>.
     *
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException thrown if pref is null.
     */
    public void rememberPreferences(Preferences pref) throws IllegalArgumentException  {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        pref.put("oauth.settings.use-default", cbUseDefaults.isSelected());
        if (cbUseDefaults.isSelected()) {
            pref.put("oauth.settings.consumer-key", null);
            pref.put("oauth.settings.consumer-secret", null);
            pref.put("oauth.settings.request-token-url", null);
            pref.put("oauth.settings.access-token-url", null);
            pref.put("oauth.settings.authorise-url", null);
        } else {
            pref.put("oauth.settings.consumer-key", tfConsumerKey.getText().trim());
            pref.put("oauth.settings.consumer-secret", tfConsumerSecret.getText().trim());
            pref.put("oauth.settings.request-token-url", tfRequestTokenURL.getText().trim());
            pref.put("oauth.settings.access-token-url", tfAccessTokenURL.getText().trim());
            pref.put("oauth.settings.authorise-url", tfAuthoriseURL.getText().trim());
        }
    }

    class UseDefaultItemListener implements ItemListener {
        private boolean enabled;

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (!enabled) return;
            switch (e.getStateChange()) {
            case ItemEvent.SELECTED:
                if (hasCustomSettings()) {
                    if (!confirmOverwriteCustomSettings()) {
                        cbUseDefaults.setSelected(false);
                        return;
                    }
                }
                resetToDefaultSettings();
                break;
            case ItemEvent.DESELECTED:
                setChildComponentsEnabled(true);
                break;
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
