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
import java.util.concurrent.Executor;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.widgets.DefaultTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * This is an UI which supports a JOSM user to get an OAuth Access Token in a fully manual process.
 *
 * @since 2746
 */
public class ManualAuthorizationUI extends AbstractAuthorizationUI {

    private final JosmTextField tfAccessTokenKey = new JosmTextField();
    private transient AccessTokenKeyValidator valAccessTokenKey;
    private final JCheckBox cbSaveToPreferences = new JCheckBox(tr("Save Access Token in preferences"));
    private final HtmlPanel pnlMessage = new HtmlPanel();
    private final transient Executor executor;

    /**
     * Constructs a new {@code ManualAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @param executor the executor used for running the HTTP requests for the authorization
     * @since 5422
     * @deprecated since 18991, use {@link ManualAuthorizationUI#ManualAuthorizationUI(String, Executor, OAuthVersion)}
     * instead.
     */
    @Deprecated
    public ManualAuthorizationUI(String apiUrl, Executor executor) {
        this(apiUrl, executor, OAuthVersion.OAuth10a);
    }

    /**
     * Constructs a new {@code ManualAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @param executor the executor used for running the HTTP requests for the authorization
     * @param oAuthVersion The OAuthVersion to use for this UI
     * @since 18991
     */
    public ManualAuthorizationUI(String apiUrl, Executor executor, OAuthVersion oAuthVersion) {
        super(null /* don't pass apiURL because setApiUrl is overridden and references a local field */,
                oAuthVersion);
        setApiUrl(apiUrl);
        this.executor = executor;
        build();
    }

    protected JPanel buildAccessTokenPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gc = new GridBagConstraints();
        AccessTokenBuilder accessTokenBuilder = new AccessTokenBuilder();

        // the access token key input field
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.gridwidth = 2;
        gc.insets = new Insets(0, 0, 5, 0);
        pnlMessage.setText("<html><body>"
                + tr("Please enter an OAuth Access Token which is authorized to access the OSM server "
                        + "''{0}''.",
                        getApiUrl()) + "</body></html>");
        pnl.add(pnlMessage, gc);

        // the access token key input field
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.gridwidth = 1;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Access Token Key:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfAccessTokenKey, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfAccessTokenKey);
        valAccessTokenKey = new AccessTokenKeyValidator(tfAccessTokenKey);
        valAccessTokenKey.validate();
        tfAccessTokenKey.getDocument().addDocumentListener(accessTokenBuilder);

        // the checkbox for saving to preferences
        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        pnl.add(cbSaveToPreferences, gc);
        cbSaveToPreferences.setSelected(OAuthAccessTokenHolder.getInstance().isSaveToPreferences());

        // filler - grab remaining space
        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth = 2;
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
        pnl.add(new JButton(actTestAccessToken));
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
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildTabbedPreferencesPanel(), BorderLayout.CENTER);
        add(buildActionsPanel(), BorderLayout.SOUTH);
    }

    @Override
    public boolean isSaveAccessTokenToPreferences() {
        return cbSaveToPreferences.isSelected();
    }

    private static class AccessTokenKeyValidator extends DefaultTextComponentValidator {
        AccessTokenKeyValidator(JTextComponent tc) {
            super(tc, tr("Please enter an Access Token Key"),
                      tr("The Access Token Key must not be empty. Please enter an Access Token Key"));
        }
    }

    class AccessTokenBuilder implements DocumentListener {

        public void build() {
            if (!valAccessTokenKey.isValid()) {
                setAccessToken(null);
            } else {
                try {
                    setAccessToken(new OAuth20Token(getOAuthParameters(), "{\"token_type\":\"bearer\", \"access_token\":\""
                            + tfAccessTokenKey.getText().trim() + "\"}"));
                } catch (OAuth20Exception e) {
                    throw new JosmRuntimeException(e);
                }
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
        TestAccessTokenAction() {
            putValue(NAME, tr("Test Access Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to test the Access Token"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            TestAccessTokenTask task = new TestAccessTokenTask(
                    ManualAuthorizationUI.this,
                    getApiUrl(),
                    getAccessToken()
            );
            executor.execute(task);
        }

        protected final void updateEnabledState() {
            setEnabled(hasAccessToken());
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(AbstractAuthorizationUI.ACCESS_TOKEN_PROP))
                return;
            updateEnabledState();
        }
    }
}
