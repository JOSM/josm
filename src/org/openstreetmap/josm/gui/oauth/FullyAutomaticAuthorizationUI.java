// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.concurrent.Executor;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.data.oauth.IOAuthToken;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * This is a UI which supports a JOSM user to get an OAuth Access Token in a fully
 * automatic process.
 *
 * @since 2746
 */
public class FullyAutomaticAuthorizationUI extends AbstractAuthorizationUI {
    private final AccessTokenInfoPanel pnlAccessTokenInfo = new AccessTokenInfoPanel();
    private OsmPrivilegesPanel pnlOsmPrivileges;
    private JPanel pnlPropertiesPanel;
    private JPanel pnlActionButtonsPanel;
    private JPanel pnlResult;
    private final transient Executor executor;

    /**
     * Builds the panel with the three privileges the user can grant JOSM
     *
     * @return constructed panel for the privileges
     */
    protected VerticallyScrollablePanel buildGrantsPanel() {
        pnlOsmPrivileges = new OsmPrivilegesPanel();
        return pnlOsmPrivileges;
    }

    /**
     * Builds the panel for entering the username and password
     *
     * @return constructed panel for the credentials
     */
    protected VerticallyScrollablePanel buildUserNamePasswordPanel() {
        VerticallyScrollablePanel pnl = new VerticallyScrollablePanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        HtmlPanel pnlMessage = new HtmlPanel();
        HTMLEditorKit kit = (HTMLEditorKit) pnlMessage.getEditorPane().getEditorKit();
        kit.getStyleSheet().addRule(
                ".warning-body {background-color:#DDFFDD; padding: 10pt; " +
                "border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
        kit.getStyleSheet().addRule("ol {margin-left: 1cm}");
        pnlMessage.setText("<html><body><p class=\"warning-body\">"
                + tr("Please enter your OSM user name and password. The password will <strong>not</strong> be saved "
                        + "in clear text in the JOSM preferences and it will be submitted to the OSM server <strong>only once</strong>. "
                        + "Subsequent data upload requests don''t use your password any more.").replace(". ", ".<br>")
                        + "</p>"
                        + "</body></html>");
        pnl.add(pnlMessage, gc);

        // the user name input field
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 3, 3);
        pnl.add(new JLabel(tr("Username: ")), gc);

        // the password input field
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 2;
        gc.gridx = 0;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Password:")), gc);

        // filler - grab remaining space
        gc.gridx = 1;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected JPanel buildPropertiesPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        JTabbedPane tpProperties = new JTabbedPane();
        tpProperties.add(buildUserNamePasswordPanel().getVerticalScrollPane());
        tpProperties.add(buildGrantsPanel().getVerticalScrollPane());
        tpProperties.add(getAdvancedPropertiesPanel().getVerticalScrollPane());
        tpProperties.setTitleAt(0, tr("Basic"));
        tpProperties.setTitleAt(1, tr("Granted rights"));
        tpProperties.setTitleAt(2, tr("Advanced OAuth properties"));

        pnl.add(tpProperties, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Builds the panel with the action button  for starting the authorisation
     *
     * @return constructed button panel
     */
    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        RunAuthorisationAction runAuthorisationAction = new RunAuthorisationAction();
        pnl.add(new JButton(runAuthorisationAction));
        return pnl;
    }

    /**
     * Builds the panel which displays the generated Access Token.
     *
     * @return constructed panel for the results
     */
    protected JPanel buildResultsPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // the message panel
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        JMultilineLabel msg = new JMultilineLabel("");
        msg.setFont(msg.getFont().deriveFont(Font.PLAIN));
        String lbl = tr("Accept Access Token");
        msg.setText(tr("<html>"
                + "You have successfully retrieved an OAuth Access Token from the OSM website. "
                + "Click on <strong>{0}</strong> to accept the token. JOSM will use it in "
                + "subsequent requests to gain access to the OSM API."
                + "</html>", lbl));
        pnl.add(msg, gc);

        // infos about the access token
        gc.gridy = 1;
        gc.insets = new Insets(5, 0, 0, 0);
        pnl.add(pnlAccessTokenInfo, gc);

        // the actions
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl1.add(new JButton(new BackAction()));
        pnl1.add(new JButton(new TestAccessTokenAction()));
        gc.gridy = 2;
        pnl.add(pnl1, gc);

        // filler - grab the remaining space
        gc.gridy = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected final void build() {
        setLayout(new BorderLayout());
        pnlPropertiesPanel = buildPropertiesPanel();
        pnlActionButtonsPanel = buildActionButtonPanel();
        pnlResult = buildResultsPanel();

        prepareUIForEnteringRequest();
    }

    /**
     * Prepares the UI for the first step in the automatic process: entering the authentication
     * and authorisation parameters.
     *
     */
    protected void prepareUIForEnteringRequest() {
        removeAll();
        add(pnlPropertiesPanel, BorderLayout.CENTER);
        add(pnlActionButtonsPanel, BorderLayout.SOUTH);
        pnlPropertiesPanel.revalidate();
        pnlActionButtonsPanel.revalidate();
        validate();
        repaint();

        setAccessToken(null);
    }

    /**
     * Prepares the UI for the second step in the automatic process: displaying the access token
     *
     */
    protected void prepareUIForResultDisplay() {
        removeAll();
        add(pnlResult, BorderLayout.CENTER);
        validate();
        repaint();
    }

    /**
     * Constructs a new {@code FullyAutomaticAuthorizationUI} for the given API URL.
     * @param apiUrl The OSM API URL
     * @param executor the executor used for running the HTTP requests for the authorization
     * @param oAuthVersion The OAuth version to use for this UI
     * @since 18991
     */
    public FullyAutomaticAuthorizationUI(String apiUrl, Executor executor, OAuthVersion oAuthVersion) {
        super(apiUrl, oAuthVersion);
        this.executor = executor;
        build();
    }

    @Override
    public boolean isSaveAccessTokenToPreferences() {
        return pnlAccessTokenInfo.isSaveToPreferences();
    }

    @Override
    protected void setAccessToken(IOAuthToken accessToken) {
        super.setAccessToken(accessToken);
        pnlAccessTokenInfo.setAccessToken(accessToken);
    }

    /**
     * Starts the authorisation process
     */
    class RunAuthorisationAction extends AbstractAction {
        RunAuthorisationAction() {
            putValue(NAME, tr("Authorize now"));
            new ImageProvider("oauth", "oauth-small").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to redirect you to the authorization form on the JOSM web site"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            executor.execute(new FullyAutomaticAuthorisationTask(FullyAutomaticAuthorizationUI.this));
        }
    }

    /**
     * Action to go back to step 1 in the process
     */
    class BackAction extends AbstractAction {
        BackAction() {
            putValue(NAME, tr("Back"));
            putValue(SHORT_DESCRIPTION, tr("Run the automatic authorization steps again"));
            new ImageProvider("dialogs", "previous").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            prepareUIForEnteringRequest();
        }
    }

    /**
     * Action to test an access token.
     */
    class TestAccessTokenAction extends AbstractAction {
        TestAccessTokenAction() {
            putValue(NAME, tr("Test Access Token"));
            new ImageProvider("logo").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            executor.execute(new TestAccessTokenTask(
                    FullyAutomaticAuthorizationUI.this,
                    getApiUrl(),
                    getAccessToken()
            ));
        }
    }

    class FullyAutomaticAuthorisationTask extends PleaseWaitRunnable {
        private boolean canceled;

        FullyAutomaticAuthorisationTask(Component parent) {
            super(parent, tr("Authorize JOSM to access the OSM API"), false /* don't ignore exceptions */);
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void finish() {
            // Do nothing
        }

        protected void alertAuthorisationFailed() {
            HelpAwareOptionPane.showOptionDialog(
                    FullyAutomaticAuthorizationUI.this,
                    tr("<html>"
                            + "The automatic process for retrieving an OAuth Access Token<br>"
                            + "from the OSM server failed.<br><br>"
                            + "Please try again or choose another kind of authorization process,<br>"
                            + "i.e. semi-automatic or manual authorization."
                            +"</html>"),
                    tr("OAuth authorization failed"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#FullyAutomaticProcessFailed")
            );
        }

        protected void alertInvalidLoginUrl() {
            HelpAwareOptionPane.showOptionDialog(
                    FullyAutomaticAuthorizationUI.this,
                    tr("<html>"
                            + "The automatic process for retrieving an OAuth Access Token<br>"
                            + "from the OSM server failed because JOSM was not able to build<br>"
                            + "a valid login URL from the OAuth Authorize Endpoint URL ''{0}''.<br><br>"
                            + "Please check your advanced setting and try again."
                            + "</html>",
                            getAdvancedPropertiesPanel().getAdvancedParameters().getAuthorizationUrl()),
                    tr("OAuth authorization failed"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/OAuthAuthorisationWizard#FullyAutomaticProcessFailed")
            );
        }

        protected void handleException(final OsmOAuthAuthorizationException e) {
            Logging.error(e);
            GuiHelper.runInEDT(this::alertAuthorisationFailed);
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            getProgressMonitor().setTicksCount(2);
            OAuthAuthorizationWizard.authorize(true, token -> {
                if (!canceled) {
                    getProgressMonitor().worked(1);
                    GuiHelper.runInEDT(() -> {
                        prepareUIForResultDisplay();
                        setAccessToken(token.orElse(null));
                    });
                }
            }, getApiUrl(), getOAuthVersion(), getOAuthParameters());
            getProgressMonitor().worked(1);
        }
    }
}
