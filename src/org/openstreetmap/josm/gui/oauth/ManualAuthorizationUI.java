// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This is an UI which supports a JOSM user to get an OAuth Access Token in a fully
 * manual process.
 *
 * @since 2746
 */
public class ManualAuthorizationUI extends AbstractAuthorizationUI{

    private JosmTextField tfAccessTokenKey;
    private transient AccessTokenKeyValidator valAccessTokenKey;
    private JosmTextField tfAccessTokenSecret;
    private transient AccessTokenSecretValidator valAccessTokenSecret;
    private JCheckBox cbSaveToPreferences;
    private HtmlPanel pnlMessage;

    protected JPanel buildAccessTokenPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gc = new GridBagConstraints();
        AccessTokenBuilder accessTokenBuilder = new AccessTokenBuilder();

        // the access token key input field
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.gridwidth = 2;
        gc.insets = new Insets(0,0,5,0);
        pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + tr("Please enter an OAuth Access Token which is authorized to access the OSM server "
                        + "''{0}''.",
                        getApiUrl()) + "</body></html>");
        pnl.add(pnlMessage, gc);

        // the access token key input field
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        gc.insets = new Insets(0,0,0,3);
        pnl.add(new JLabel(tr("Access Token Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfAccessTokenKey = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenKey);
        valAccessTokenKey = new AccessTokenKeyValidator(tfAccessTokenKey);
        valAccessTokenKey.validate();
        tfAccessTokenKey.getDocument().addDocumentListener(accessTokenBuilder);

        // the access token key input field
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Access Token Secret:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfAccessTokenSecret = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenSecret);
        valAccessTokenSecret = new AccessTokenSecretValidator(tfAccessTokenSecret);
        valAccessTokenSecret.validate();
        tfAccessTokenSecret.getDocument().addDocumentListener(accessTokenBuilder);

        // the checkbox for saving to preferences
        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth =2;
        gc.weightx = 1.0;
        pnl.add(cbSaveToPreferences = new JCheckBox(tr("Save Access Token in preferences")), gc);
        cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

        // filler - grab remaining space
        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth =2;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    protected JPanel buildTabbedPreferencesPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        JTabbedPane tp = new JTabbedPane();
        tp.add(buildAccessTokenPanel());
        tp.add(getAdvancedPropertiesPanel());

        tp.setTitleAt(0, tr("Access Token"));
        tp.setTitleAt(1, tr("Advanced OAuth parameters"));

        tp.setToolTipTextAt(0, tr("Enter the OAuth Access Token"));
        tp.setToolTipTextAt(1, tr("Enter advanced OAuth properties"));

        pnl.add(tp, BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildActionsPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        TestAccessTokenAction actTestAccessToken = new TestAccessTokenAction();
        pnl.add(new SideButton(actTestAccessToken));
        this.addPropertyChangeListener(actTestAccessToken);
        return pnl;
    }

    @Override
    public void setApiUrl(String apiUrl) {
        super.setApiUrl(apiUrl);
        if (pnlMessage != null) {
            pnlMessage.setText(tr("<html><body>"
                    + "Please enter an OAuth Access Token which is authorized to access the OSM server "
                    + "''{0}''."
                    + "</body></html>",
                    getApiUrl()
            ));
        }
    }

    protected final void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(buildTabbedPreferencesPanel(), BorderLayout.CENTER);
        add(buildActionsPanel(), BorderLayout.SOUTH);
    }

    /**
     * Constructs a new {@code ManualAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @since 5422
     */
    public ManualAuthorizationUI(String apiUrl) {
        super(apiUrl);
        build();
    }

    @Override
    public boolean isSaveAccessTokenToPreferences() {
        return cbSaveToPreferences.isSelected();
    }

    private static class AccessTokenKeyValidator extends AbstractTextComponentValidator {

        public AccessTokenKeyValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return !getComponent().getText().trim().isEmpty();
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter an Access Token Key"));
            } else {
                feedbackInvalid(tr("The Access Token Key must not be empty. Please enter an Access Token Key"));
            }
        }
    }

    private static class AccessTokenSecretValidator extends AbstractTextComponentValidator {
        public AccessTokenSecretValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return !getComponent().getText().trim().isEmpty();
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter an Access Token Secret"));
            } else {
                feedbackInvalid(tr("The Access Token Secret must not be empty. Please enter an Access Token Secret"));
            }
        }
    }

    class AccessTokenBuilder implements DocumentListener {

        public void build() {
            if (! valAccessTokenKey.isValid() || !valAccessTokenSecret.isValid()) {
                setAccessToken(null);
            } else {
                setAccessToken(new OAuthToken(tfAccessTokenKey.getText().trim(), tfAccessTokenSecret.getText().trim()));
            }
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            build();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            build();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            build();
        }
    }

    /**
     * Action for testing an Access Token
     */
    class TestAccessTokenAction extends AbstractAction implements PropertyChangeListener {
        public TestAccessTokenAction() {
            putValue(NAME, tr("Test Access Token"));
            putValue(SMALL_ICON, ImageProvider.get("oauth", "oauth"));
            putValue(SHORT_DESCRIPTION, tr("Click to test the Access Token"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            TestAccessTokenTask task = new TestAccessTokenTask(
                    ManualAuthorizationUI.this,
                    getApiUrl(),
                    getAdvancedPropertiesPanel().getAdvancedParameters(),
                    getAccessToken()
            );
            Main.worker.submit(task);
        }

        protected final void updateEnabledState() {
            setEnabled(hasAccessToken());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (! evt.getPropertyName().equals(AbstractAuthorizationUI.ACCESS_TOKEN_PROP))
                return;
            updateEnabledState();
        }
    }
}
