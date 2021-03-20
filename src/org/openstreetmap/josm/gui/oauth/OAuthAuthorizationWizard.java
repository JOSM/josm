// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * This wizard walks the user to the necessary steps to retrieve an OAuth Access Token which
 * allows JOSM to access the OSM API on the users behalf.
 * @since 2746
 */
public class OAuthAuthorizationWizard extends JDialog {
    private boolean canceled;
    private final AuthorizationProcedure procedure;
    private final String apiUrl;

    private FullyAutomaticAuthorizationUI pnlFullyAutomaticAuthorisationUI;
    private SemiAutomaticAuthorizationUI pnlSemiAutomaticAuthorisationUI;
    private ManualAuthorizationUI pnlManualAuthorisationUI;
    private JScrollPane spAuthorisationProcedureUI;
    private final transient Executor executor;

    /**
     * Launches the wizard, {@link OAuthAccessTokenHolder#setAccessToken(OAuthToken) sets the token}
     * and {@link OAuthAccessTokenHolder#setSaveToPreferences(boolean) saves to preferences}.
     * @throws UserCancelException if user cancels the operation
     */
    public void showDialog() throws UserCancelException {
        setVisible(true);
        if (isCanceled()) {
            throw new UserCancelException();
        }
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        holder.setAccessToken(getAccessToken());
        holder.setSaveToPreferences(isSaveAccessTokenToPreferences());
    }

    /**
     * Builds the row with the action buttons
     *
     * @return panel with buttons
     */
    protected JPanel buildButtonRow() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        AcceptAccessTokenAction actAcceptAccessToken = new AcceptAccessTokenAction();
        pnlFullyAutomaticAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);
        pnlSemiAutomaticAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);
        pnlManualAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);

        pnl.add(new JButton(actAcceptAccessToken));
        pnl.add(new JButton(new CancelAction()));
        pnl.add(new JButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/OAuthAuthorisationWizard"))));

        return pnl;
    }

    /**
     * Builds the panel with general information in the header
     *
     * @return panel with information display
     */
    protected JPanel buildHeaderInfoPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // OAuth in a nutshell ...
        HtmlPanel pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + tr("With OAuth you grant JOSM the right to upload map data and GPS tracks "
                        + "on your behalf (<a href=\"{0}\">more info...</a>).", "https://wiki.openstreetmap.org/wiki/OAuth")
                        + "</body></html>"
        );
        pnlMessage.enableClickableHyperlinks();
        pnl.add(pnlMessage, GBC.eol().fill(GBC.HORIZONTAL));

        // the authorisation procedure
        JMultilineLabel lbl = new JMultilineLabel(AuthorizationProcedure.FULLY_AUTOMATIC.getDescription());
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
        pnl.add(lbl, GBC.std());

        if (!Config.getUrls().getDefaultOsmApiUrl().equals(apiUrl)) {
            final HtmlPanel pnlWarning = new HtmlPanel();
            final HTMLEditorKit kit = (HTMLEditorKit) pnlWarning.getEditorPane().getEditorKit();
            kit.getStyleSheet().addRule(".warning-body {"
                    + "background-color:rgb(253,255,221);padding: 10pt; "
                    + "border-color:rgb(128,128,128);border-style: solid;border-width: 1px;}");
            kit.getStyleSheet().addRule("ol {margin-left: 1cm}");
            pnlWarning.setText("<html><body>"
                    + "<p class=\"warning-body\">"
                    + tr("<strong>Warning:</strong> Since you are using not the default OSM API, " +
                    "make sure to set an OAuth consumer key and secret in the <i>Advanced OAuth parameters</i>.")
                    + "</p>"
                    + "</body></html>");
            pnl.add(pnlWarning, GBC.eop().fill());
        }

        return pnl;
    }

    /**
     * Refreshes the view of the authorisation panel, depending on the authorisation procedure
     * currently selected
     */
    protected void refreshAuthorisationProcedurePanel() {
        switch(procedure) {
        case FULLY_AUTOMATIC:
            spAuthorisationProcedureUI.getViewport().setView(pnlFullyAutomaticAuthorisationUI);
            pnlFullyAutomaticAuthorisationUI.revalidate();
            break;
        case SEMI_AUTOMATIC:
            spAuthorisationProcedureUI.getViewport().setView(pnlSemiAutomaticAuthorisationUI);
            pnlSemiAutomaticAuthorisationUI.revalidate();
            break;
        case MANUALLY:
            spAuthorisationProcedureUI.getViewport().setView(pnlManualAuthorisationUI);
            pnlManualAuthorisationUI.revalidate();
            break;
        }
        validate();
        repaint();
    }

    /**
     * builds the UI
     */
    protected final void build() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildHeaderInfoPanel(), BorderLayout.NORTH);

        setTitle(tr("Get an Access Token for ''{0}''", apiUrl));
        this.setMinimumSize(new Dimension(500, 300));

        pnlFullyAutomaticAuthorisationUI = new FullyAutomaticAuthorizationUI(apiUrl, executor);
        pnlSemiAutomaticAuthorisationUI = new SemiAutomaticAuthorizationUI(apiUrl, executor);
        pnlManualAuthorisationUI = new ManualAuthorizationUI(apiUrl, executor);

        spAuthorisationProcedureUI = GuiHelper.embedInVerticalScrollPane(new JPanel());
        spAuthorisationProcedureUI.getVerticalScrollBar().addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent e) {
                        spAuthorisationProcedureUI.setBorder(UIManager.getBorder("ScrollPane.border"));
                    }

                    @Override
                    public void componentHidden(ComponentEvent e) {
                        spAuthorisationProcedureUI.setBorder(null);
                    }
                }
        );
        getContentPane().add(spAuthorisationProcedureUI, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());
        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());

        refreshAuthorisationProcedurePanel();

        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/OAuthAuthorisationWizard"));
    }

    /**
     * Creates the wizard.
     *
     * @param parent the component relative to which the dialog is displayed
     * @param procedure the authorization procedure to use
     * @param apiUrl the API URL. Must not be null.
     * @param executor the executor used for running the HTTP requests for the authorization
     * @throws IllegalArgumentException if apiUrl is null
     */
    public OAuthAuthorizationWizard(Component parent, AuthorizationProcedure procedure, String apiUrl, Executor executor) {
        super(GuiHelper.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        this.procedure = Objects.requireNonNull(procedure, "procedure");
        this.apiUrl = Objects.requireNonNull(apiUrl, "apiUrl");
        this.executor = executor;
        build();
    }

    /**
     * Replies true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected AbstractAuthorizationUI getCurrentAuthorisationUI() {
        switch(procedure) {
        case FULLY_AUTOMATIC: return pnlFullyAutomaticAuthorisationUI;
        case MANUALLY: return pnlManualAuthorisationUI;
        case SEMI_AUTOMATIC: return pnlSemiAutomaticAuthorisationUI;
        default: return null;
        }
    }

    /**
     * Replies the Access Token entered using the wizard
     *
     * @return the access token. May be null if the wizard was canceled.
     */
    public OAuthToken getAccessToken() {
        return getCurrentAuthorisationUI().getAccessToken();
    }

    /**
     * Replies the current OAuth parameters.
     *
     * @return the current OAuth parameters.
     */
    public OAuthParameters getOAuthParameters() {
        return getCurrentAuthorisationUI().getOAuthParameters();
    }

    /**
     * Replies true if the currently selected Access Token shall be saved to
     * the preferences.
     *
     * @return true if the currently selected Access Token shall be saved to
     * the preferences
     */
    public boolean isSaveAccessTokenToPreferences() {
        return getCurrentAuthorisationUI().isSaveAccessTokenToPreferences();
    }

    /**
     * Initializes the dialog with values from the preferences
     *
     */
    public void initFromPreferences() {
        pnlFullyAutomaticAuthorisationUI.initialize(apiUrl);
        pnlSemiAutomaticAuthorisationUI.initialize(apiUrl);
        pnlManualAuthorisationUI.initialize(apiUrl);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            pack();
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            MainApplication.getMainFrame(),
                            getPreferredSize()
                    )
            ).applySafe(this);
            initFromPreferences();
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Obtains an OAuth access token for the connection. Afterwards, the token is accessible via {@link OAuthAccessTokenHolder}.
     * @param serverUrl the URL to OSM server
     * @throws InterruptedException if we're interrupted while waiting for the event dispatching thread to finish OAuth authorization task
     * @throws InvocationTargetException if an exception is thrown while running OAuth authorization task
     * @since 12803
     */
    public static void obtainAccessToken(final URL serverUrl) throws InvocationTargetException, InterruptedException {
        final Runnable authTask = new FutureTask<>(() -> {
            // Concerning Utils.newDirectExecutor: Main worker cannot be used since this connection is already
            // executed via main worker. The OAuth connections would block otherwise.
            final OAuthAuthorizationWizard wizard = new OAuthAuthorizationWizard(
                    MainApplication.getMainFrame(),
                    AuthorizationProcedure.FULLY_AUTOMATIC,
                    serverUrl.toExternalForm(), Utils.newDirectExecutor());
            wizard.showDialog();
            return wizard;
        });
        // exception handling differs from implementation at GuiHelper.runInEDTAndWait()
        if (SwingUtilities.isEventDispatchThread()) {
            authTask.run();
        } else {
            SwingUtilities.invokeAndWait(authTask);
        }
    }

    class CancelAction extends AbstractAction {

        /**
         * Constructs a new {@code CancelAction}.
         */
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and cancel authorization"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            cancel();
        }
    }

    class AcceptAccessTokenAction extends AbstractAction implements PropertyChangeListener {

        /**
         * Constructs a new {@code AcceptAccessTokenAction}.
         */
        AcceptAccessTokenAction() {
            putValue(NAME, tr("Accept Access Token"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and accept the Access Token"));
            updateEnabledState(null);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            setCanceled(false);
            setVisible(false);
        }

        public final void updateEnabledState(OAuthToken token) {
            setEnabled(token != null);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(AbstractAuthorizationUI.ACCESS_TOKEN_PROP))
                return;
            updateEnabledState((OAuthToken) evt.getNewValue());
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            new CancelAction().cancel();
        }
    }
}
