// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

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
import java.util.concurrent.Executor;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * This is the UI for running a semic-automic authorisation procedure.
 *
 * In contrast to the fully-automatic procedure the user is dispatched to an
 * external browser for login and authorisation.
 *
 * @since 2746
 */
public class SemiAutomaticAuthorizationUI extends AbstractAuthorizationUI {
    private final AccessTokenInfoPanel pnlAccessTokenInfo = new AccessTokenInfoPanel();
    private transient OAuthToken requestToken;

    private RetrieveRequestTokenPanel pnlRetrieveRequestToken;
    private RetrieveAccessTokenPanel pnlRetrieveAccessToken;
    private ShowAccessTokenPanel pnlShowAccessToken;
    private final transient Executor executor;

    /**
     * build the UI
     */
    protected final void build() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlRetrieveRequestToken = new RetrieveRequestTokenPanel();
        pnlRetrieveAccessToken = new RetrieveAccessTokenPanel();
        pnlShowAccessToken = new ShowAccessTokenPanel();
        add(pnlRetrieveRequestToken, BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code SemiAutomaticAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @param executor the executor used for running the HTTP requests for the authorization
     * @since 5422
     */
    public SemiAutomaticAuthorizationUI(String apiUrl, Executor executor) {
        super(apiUrl);
        this.executor = executor;
        build();
    }

    @Override
    public boolean isSaveAccessTokenToPreferences() {
        return pnlAccessTokenInfo.isSaveToPreferences();
    }

    protected void transitionToRetrieveAccessToken() {
        OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(
                getAdvancedPropertiesPanel().getAdvancedParameters()
        );
        String authoriseUrl = client.getAuthoriseUrl(requestToken);
        OpenBrowser.displayUrl(authoriseUrl);

        removeAll();
        pnlRetrieveAccessToken.setAuthoriseUrl(authoriseUrl);
        add(pnlRetrieveAccessToken, BorderLayout.CENTER);
        pnlRetrieveAccessToken.invalidate();
        validate();
        repaint();
    }

    protected void transitionToRetrieveRequestToken() {
        requestToken = null;
        setAccessToken(null);
        removeAll();
        add(pnlRetrieveRequestToken, BorderLayout.CENTER);
        pnlRetrieveRequestToken.invalidate();
        validate();
        repaint();
    }

    protected void transitionToShowAccessToken() {
        removeAll();
        add(pnlShowAccessToken, BorderLayout.CENTER);
        pnlShowAccessToken.invalidate();
        validate();
        repaint();
        pnlShowAccessToken.setAccessToken(getAccessToken());
    }

    static class StepLabel extends JLabel {
        StepLabel(String text) {
            super(text);
            setFont(getFont().deriveFont(16f));
        }
    }

    /**
     * This is the panel displayed in the first step of the semi-automatic authorisation process.
     */
    private class RetrieveRequestTokenPanel extends JPanel {

        /**
         * Constructs a new {@code RetrieveRequestTokenPanel}.
         */
        RetrieveRequestTokenPanel() {
            build();
        }

        protected JPanel buildAdvancedParametersPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();

            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0, 0, 0, 3);
            JCheckBox cbShowAdvancedParameters = new JCheckBox();
            pnl.add(cbShowAdvancedParameters, gc);
            cbShowAdvancedParameters.setSelected(false);
            cbShowAdvancedParameters.addItemListener(
                    evt -> getAdvancedPropertiesPanel().setVisible(evt.getStateChange() == ItemEvent.SELECTED)
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
            pnl.add(getAdvancedPropertiesPanel(), gc);
            getAdvancedPropertiesPanel().setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 1),
                            BorderFactory.createEmptyBorder(3, 3, 3, 3)
                    )
            );
            getAdvancedPropertiesPanel().setVisible(false);
            return pnl;
        }

        protected JPanel buildCommandPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();

            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 1.0;
            gc.insets = new Insets(0, 0, 0, 3);


            HtmlPanel h = new HtmlPanel();
            h.setText(tr("<html>"
                    + "Please click on <strong>{0}</strong> to retrieve an OAuth Request Token from "
                    + "''{1}''.</html>",
                    tr("Retrieve Request Token"),
                    getAdvancedPropertiesPanel().getAdvancedParameters().getRequestTokenUrl()
            ));
            pnl.add(h, gc);

            JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            pnl1.add(new JButton(new RetrieveRequestTokenAction()));
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridy = 1;
            pnl.add(pnl1, gc);
            return pnl;

        }

        protected final void build() {
            setLayout(new BorderLayout(0, 5));
            add(new StepLabel(tr("<html>Step 1/3: Retrieve an OAuth Request Token</html>")), BorderLayout.NORTH);
            add(buildAdvancedParametersPanel(), BorderLayout.CENTER);
            add(buildCommandPanel(), BorderLayout.SOUTH);
        }
    }

    /**
     * This is the panel displayed in the second step of the semi-automatic authorization process.
     */
    private class RetrieveAccessTokenPanel extends JPanel {

        private final JosmTextField tfAuthoriseUrl = new JosmTextField(null, null, 0, false);

        /**
         * Constructs a new {@code RetrieveAccessTokenPanel}.
         */
        RetrieveAccessTokenPanel() {
            build();
        }

        protected JPanel buildTitlePanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.add(new StepLabel(tr("<html>Step 2/3: Authorize and retrieve an Access Token</html>")), BorderLayout.CENTER);
            return pnl;
        }

        protected JPanel buildContentPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();

            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridwidth = 2;
            HtmlPanel html = new HtmlPanel();
            html.setText(tr("<html>"
                    + "JOSM successfully retrieved a Request Token. "
                    + "JOSM is now launching an authorization page in an external browser. "
                    + "Please login with your OSM username and password and follow the instructions "
                    + "to authorize the Request Token. Then switch back to this dialog and click on "
                    + "<strong>{0}</strong><br><br>"
                    + "If launching the external browser fails you can copy the following authorize URL "
                    + "and paste it into the address field of your browser.</html>",
                    tr("Request Access Token")
            ));
            pnl.add(html, gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.weightx = 0.0;
            gc.gridwidth = 1;
            pnl.add(new JLabel(tr("Authorize URL:")), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            pnl.add(tfAuthoriseUrl, gc);
            tfAuthoriseUrl.setEditable(false);

            return pnl;
        }

        protected JPanel buildActionPanel() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            pnl.add(new JButton(new BackAction()));
            pnl.add(new JButton(new RetrieveAccessTokenAction()));
            return pnl;
        }

        protected final void build() {
            setLayout(new BorderLayout());
            add(buildTitlePanel(), BorderLayout.NORTH);
            add(buildContentPanel(), BorderLayout.CENTER);
            add(buildActionPanel(), BorderLayout.SOUTH);
        }

        public void setAuthoriseUrl(String url) {
            tfAuthoriseUrl.setText(url);
        }

        /**
         * Action to go back to step 1 in the process
         */
        class BackAction extends AbstractAction {
            BackAction() {
                putValue(NAME, tr("Back"));
                putValue(SHORT_DESCRIPTION, tr("Go back to step 1/3"));
                new ImageProvider("dialogs", "previous").getResource().attachImageIcon(this);
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                transitionToRetrieveRequestToken();
            }
        }
    }

    /**
     * Displays the retrieved Access Token in step 3.
     */
    class ShowAccessTokenPanel extends JPanel {

        /**
         * Constructs a new {@code ShowAccessTokenPanel}.
         */
        ShowAccessTokenPanel() {
            build();
        }

        protected JPanel buildTitlePanel() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.add(new StepLabel(tr("<html>Step 3/3: Successfully retrieved an Access Token</html>")), BorderLayout.CENTER);
            return pnl;
        }

        protected JPanel buildContentPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();

            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            HtmlPanel html = new HtmlPanel();
            html.setText(tr("<html>"
                    + "JOSM has successfully retrieved an Access Token. "
                    + "You can now accept this token. JOSM will use it in the future for authentication "
                    + "and authorization to the OSM server.<br><br>"
                    + "The access token is: </html>"
            ));
            pnl.add(html, gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.weightx = 1.0;
            gc.gridwidth = 1;
            pnl.add(pnlAccessTokenInfo, gc);
            pnlAccessTokenInfo.setSaveToPreferences(
                    OAuthAccessTokenHolder.getInstance().isSaveToPreferences()
            );
            return pnl;
        }

        protected JPanel buildActionPanel() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
            pnl.add(new JButton(new RestartAction()));
            pnl.add(new JButton(new TestAccessTokenAction()));
            return pnl;
        }

        protected final void build() {
            setLayout(new BorderLayout());
            add(buildTitlePanel(), BorderLayout.NORTH);
            add(buildContentPanel(), BorderLayout.CENTER);
            add(buildActionPanel(), BorderLayout.SOUTH);
        }

        /**
         * Action to go back to step 1 in the process
         */
        class RestartAction extends AbstractAction {
            RestartAction() {
                putValue(NAME, tr("Restart"));
                putValue(SHORT_DESCRIPTION, tr("Go back to step 1/3"));
                new ImageProvider("dialogs", "previous").getResource().attachImageIcon(this);
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                transitionToRetrieveRequestToken();
            }
        }

        public void setAccessToken(OAuthToken accessToken) {
            pnlAccessTokenInfo.setAccessToken(accessToken);
        }
    }

    /**
     * Action for retrieving a request token
     */
    class RetrieveRequestTokenAction extends AbstractAction {

        RetrieveRequestTokenAction() {
            putValue(NAME, tr("Retrieve Request Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to retrieve a Request Token"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final RetrieveRequestTokenTask task = new RetrieveRequestTokenTask(
                    SemiAutomaticAuthorizationUI.this,
                    getAdvancedPropertiesPanel().getAdvancedParameters()
            );
            executor.execute(task);
            Runnable r = () -> {
                if (task.isCanceled()) return;
                if (task.getRequestToken() == null) return;
                requestToken = task.getRequestToken();
                GuiHelper.runInEDT(SemiAutomaticAuthorizationUI.this::transitionToRetrieveAccessToken);
            };
            executor.execute(r);
        }
    }

    /**
     * Action for retrieving an Access Token
     */
    class RetrieveAccessTokenAction extends AbstractAction {

        RetrieveAccessTokenAction() {
            putValue(NAME, tr("Retrieve Access Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to retrieve an Access Token"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            final RetrieveAccessTokenTask task = new RetrieveAccessTokenTask(
                    SemiAutomaticAuthorizationUI.this,
                    getAdvancedPropertiesPanel().getAdvancedParameters(),
                    requestToken
            );
            executor.execute(task);
            Runnable r = () -> {
                if (task.isCanceled()) return;
                if (task.getAccessToken() == null) return;
                GuiHelper.runInEDT(() -> {
                    setAccessToken(task.getAccessToken());
                    transitionToShowAccessToken();
                });
            };
            executor.execute(r);
        }
    }

    /**
     * Action for testing an Access Token
     */
    class TestAccessTokenAction extends AbstractAction {

        TestAccessTokenAction() {
            putValue(NAME, tr("Test Access Token"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to test the Access Token"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            TestAccessTokenTask task = new TestAccessTokenTask(
                    SemiAutomaticAuthorizationUI.this,
                    getApiUrl(),
                    getAdvancedPropertiesPanel().getAdvancedParameters(),
                    getAccessToken()
            );
            executor.execute(task);
        }
    }
}
