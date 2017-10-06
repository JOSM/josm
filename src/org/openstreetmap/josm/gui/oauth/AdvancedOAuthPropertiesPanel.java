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
    private transient UseDefaultItemListener ilUseDefault;
    private String apiUrl;

    /**
     * Constructs a new {@code AdvancedOAuthPropertiesPanel}.
     */
    public AdvancedOAuthPropertiesPanel() {
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
        gc.gridwidth = 2;
        add(cbUseDefaults, gc);

        // -- consumer key
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        add(new JLabel(tr("Consumer Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerKey, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerKey);

        // -- consumer secret
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Consumer Secret:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfConsumerSecret, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfConsumerSecret);

        // -- request token URL
        gc.gridy = 3;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Request Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfRequestTokenURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfRequestTokenURL);

        // -- access token URL
        gc.gridy = 4;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Access Token URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAccessTokenURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenURL);

        // -- authorise URL
        gc.gridy = 5;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("Authorize URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfAuthoriseURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAuthoriseURL);

        // -- OSM login URL
        gc.gridy = 6;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("OSM login URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmLoginURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmLoginURL);

        // -- OSM logout URL
        gc.gridy = 7;
        gc.gridx = 0;
        gc.weightx = 0.0;
        add(new JLabel(tr("OSM logout URL:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        add(tfOsmLogoutURL, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfOsmLogoutURL);

        ilUseDefault = new UseDefaultItemListener();
        cbUseDefaults.addItemListener(ilUseDefault);
    }

    protected boolean hasCustomSettings() {
        OAuthParameters params = OAuthParameters.createDefault(apiUrl);
        return !params.equals(getAdvancedParameters());
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
        OAuthParameters params = OAuthParameters.createDefault(apiUrl);
        tfConsumerKey.setText(params.getConsumerKey());
        tfConsumerSecret.setText(params.getConsumerSecret());
        tfRequestTokenURL.setText(params.getRequestTokenUrl());
        tfAccessTokenURL.setText(params.getAccessTokenUrl());
        tfAuthoriseURL.setText(params.getAuthoriseUrl());
        tfOsmLoginURL.setText(params.getOsmLoginUrl());
        tfOsmLogoutURL.setText(params.getOsmLogoutUrl());

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
    public OAuthParameters getAdvancedParameters() {
        if (cbUseDefaults.isSelected())
            return OAuthParameters.createDefault(apiUrl);
        return new OAuthParameters(
            tfConsumerKey.getText(),
            tfConsumerSecret.getText(),
            tfRequestTokenURL.getText(),
            tfAccessTokenURL.getText(),
            tfAuthoriseURL.getText(),
            tfOsmLoginURL.getText(),
            tfOsmLogoutURL.getText());
    }

    /**
     * Sets the advanced parameters to be displayed
     *
     * @param parameters the advanced parameters. Must not be null.
     * @throws IllegalArgumentException if parameters is null.
     */
    public void setAdvancedParameters(OAuthParameters parameters) {
        CheckParameterUtil.ensureParameterNotNull(parameters, "parameters");
        if (parameters.equals(OAuthParameters.createDefault(apiUrl))) {
            cbUseDefaults.setSelected(true);
            setChildComponentsEnabled(false);
        } else {
            cbUseDefaults.setSelected(false);
            setChildComponentsEnabled(true);
            tfConsumerKey.setText(parameters.getConsumerKey() == null ? "" : parameters.getConsumerKey());
            tfConsumerSecret.setText(parameters.getConsumerSecret() == null ? "" : parameters.getConsumerSecret());
            tfRequestTokenURL.setText(parameters.getRequestTokenUrl() == null ? "" : parameters.getRequestTokenUrl());
            tfAccessTokenURL.setText(parameters.getAccessTokenUrl() == null ? "" : parameters.getAccessTokenUrl());
            tfAuthoriseURL.setText(parameters.getAuthoriseUrl() == null ? "" : parameters.getAuthoriseUrl());
            tfOsmLoginURL.setText(parameters.getOsmLoginUrl() == null ? "" : parameters.getOsmLoginUrl());
            tfOsmLogoutURL.setText(parameters.getOsmLogoutUrl() == null ? "" : parameters.getOsmLogoutUrl());
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
     * Initializes the panel from the values in the preferences <code>preferences</code>.
     *
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException if pref is null
     * @deprecated (since 12928) replaced by {@link #initialize(java.lang.String)}
     */
    @Deprecated
    public void initFromPreferences(Preferences pref) {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        setApiUrl(pref.get("osm-server.url"));
        boolean useDefault = pref.getBoolean("oauth.settings.use-default", true);
        ilUseDefault.setEnabled(false);
        if (useDefault) {
            resetToDefaultSettings();
        } else {
            setAdvancedParameters(OAuthParameters.createFromPreferences(pref));
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

    /**
     * Remembers the current values in the preferences <code>pref</code>.
     *
     * @param pref the preferences. Must not be null.
     * @throws IllegalArgumentException if pref is null.
     * @deprecated (since 12928) replaced by {@link #rememberPreferences()}
     */
    @Deprecated
    public void rememberPreferences(Preferences pref) {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        pref.putBoolean("oauth.settings.use-default", cbUseDefaults.isSelected());
        if (cbUseDefaults.isSelected()) {
            new OAuthParameters(null, null, null, null, null, null, null).rememberPreferences(pref);
        } else {
            getAdvancedParameters().rememberPreferences(pref);
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
