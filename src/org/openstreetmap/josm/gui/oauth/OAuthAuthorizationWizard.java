// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executor;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This wizard walks the user to the necessary steps to retrieve an OAuth Access Token which
 * allows JOSM to access the OSM API on the users behalf.
 * @since 2746
 */
public class OAuthAuthorizationWizard extends JDialog {
    private boolean canceled;
    private final String apiUrl;

    private final AuthorizationProcedureComboBox cbAuthorisationProcedure = new AuthorizationProcedureComboBox();
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
        GridBagConstraints gc = new GridBagConstraints();

        // the oauth logo in the header
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        ImageProvider logoProv = new ImageProvider("oauth", "oauth-logo").setMaxHeight(100);
        JLabel lbl = new JLabel(logoProv.get());
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        lbl.setOpaque(true);
        pnl.add(lbl, gc);

        // OAuth in a nutshell ...
        gc.gridy = 1;
        gc.insets = new Insets(5, 0, 0, 5);
        HtmlPanel pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + tr("With OAuth you grant JOSM the right to upload map data and GPS tracks "
                        + "on your behalf (<a href=\"{0}\">more info...</a>).", "http://oauth.net/")
                        + "</body></html>"
        );
        pnlMessage.getEditorPane().addHyperlinkListener(new ExternalBrowserLauncher());
        pnl.add(pnlMessage, gc);

        // the authorisation procedure
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        lbl = new JLabel(tr("Please select an authorization procedure: "));
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
        pnl.add(lbl, gc);

        gc.gridx = 1;
        gc.gridwidth = 1;
        gc.weightx = 1.0;
        pnl.add(cbAuthorisationProcedure, gc);
        cbAuthorisationProcedure.addItemListener(new AuthorisationProcedureChangeListener());
        lbl.setLabelFor(cbAuthorisationProcedure);

        if (!OsmApi.DEFAULT_API_URL.equals(apiUrl)) {
            gc.gridy = 3;
            gc.gridwidth = 2;
            gc.gridx = 0;
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
            pnl.add(pnlWarning, gc);
        }

        return pnl;
    }

    /**
     * Refreshes the view of the authorisation panel, depending on the authorisation procedure
     * currently selected
     */
    protected void refreshAuthorisationProcedurePanel() {
        AuthorizationProcedure procedure = (AuthorizationProcedure) cbAuthorisationProcedure.getSelectedItem();
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
        this.setMinimumSize(new Dimension(600, 420));

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
     * @param apiUrl the API URL. Must not be null.
     * @param executor the executor used for running the HTTP requests for the authorization
     * @throws IllegalArgumentException if apiUrl is null
     */
    public OAuthAuthorizationWizard(Component parent, String apiUrl, Executor executor) {
        super(GuiHelper.getFrameForComponent(parent), ModalityType.DOCUMENT_MODAL);
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        this.apiUrl = apiUrl;
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
        switch((AuthorizationProcedure) cbAuthorisationProcedure.getSelectedItem()) {
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
        // Copy current JOSM preferences to update API url with the one used in this wizard
        Preferences copyPref = new Preferences(Main.pref);
        copyPref.put("osm-server.url", apiUrl);
        pnlFullyAutomaticAuthorisationUI.initFromPreferences(copyPref);
        pnlSemiAutomaticAuthorisationUI.initFromPreferences(copyPref);
        pnlManualAuthorisationUI.initFromPreferences(copyPref);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(450, 540)
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

    class AuthorisationProcedureChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            refreshAuthorisationProcedurePanel();
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

    static class ExternalBrowserLauncher implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                OpenBrowser.displayUrl(e.getDescription());
            }
        }
    }
}
