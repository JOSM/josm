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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This wizard walks the user to the necessary steps to retrieve an OAuth Access Token which
 * allows JOSM to access the OSM API on the users behalf.
 * 
 */
public class OAuthAuthorisationWizard extends JDialog {
    static private final Logger logger = Logger.getLogger(OAuthAuthorisationWizard.class.getName());

    private HtmlPanel pnlMessage;
    private boolean canceled;
    private String apiUrl;

    private AuthorisationProcedureComboBox cbAuthorisationProcedure;
    private FullyAutomaticAuthorisationUI pnlFullyAutomaticAuthorisationUI;
    private SemiAutomaticAuthorisationUI pnlSemiAutomaticAuthorisationUI;
    private ManualAuthorisationUI pnlManualAuthorisationUI;
    private JScrollPane spAuthorisationProcedureUI;

    /**
     * Builds the row with the action buttons
     * 
     * @return
     */
    protected JPanel buildButtonRow(){
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        AcceptAccessTokenAction actAcceptAccessToken = new AcceptAccessTokenAction();
        pnlFullyAutomaticAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);
        pnlSemiAutomaticAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);
        pnlManualAuthorisationUI.addPropertyChangeListener(actAcceptAccessToken);

        pnl.add(new SideButton(actAcceptAccessToken));
        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/OAuthAuthorisationWizard"))));

        return pnl;
    }

    /**
     * Builds the panel with general information in the header
     * 
     * @return
     */
    protected JPanel buildHeaderInfoPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gc = new GridBagConstraints();

        // the oauth logo in the header
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        JLabel lbl = new JLabel();
        lbl.setIcon(ImageProvider.get("oauth", "oauth-logo"));
        lbl.setOpaque(true);
        pnl.add(lbl, gc);

        // OAuth in a nutshell ...
        gc.gridy  = 1;
        gc.insets = new Insets(5,0,0,5);
        pnlMessage = new HtmlPanel();
        pnlMessage.setText("<html><body>"
                + "With OAuth you grant JOSM the right to upload map data and GPS tracks "
                + "on your behalf (<a href=\"urn:josm-oauth-info\">more info...</a>)."
                + "</body></html>"
        );
        pnl.add(pnlMessage, gc);

        // the authorisation procedure
        gc.gridy  = 2;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        lbl = new JLabel(tr("Please select an authorisation procedure: "));
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
        pnl.add(lbl,gc);

        gc.gridx = 1;
        gc.gridwidth = 1;
        gc.weightx = 1.0;
        pnl.add(cbAuthorisationProcedure = new AuthorisationProcedureComboBox(),gc);
        cbAuthorisationProcedure.addItemListener(new AuthorisationProcedureChangeListener());
        return pnl;
    }

    /**
     * Refreshes the view of the authorisation panel, depending on the authorisation procedure
     * currently selected
     */
    protected void refreshAuthorisationProcedurePanel() {
        AuthorisationProcedure procedure = (AuthorisationProcedure)cbAuthorisationProcedure.getSelectedItem();
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
    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildHeaderInfoPanel(), BorderLayout.NORTH);

        pnlFullyAutomaticAuthorisationUI = new FullyAutomaticAuthorisationUI();
        pnlFullyAutomaticAuthorisationUI.setApiUrl(apiUrl);

        pnlSemiAutomaticAuthorisationUI = new SemiAutomaticAuthorisationUI();
        pnlSemiAutomaticAuthorisationUI.setApiUrl(apiUrl);

        pnlManualAuthorisationUI = new ManualAuthorisationUI();
        pnlManualAuthorisationUI.setApiUrl(apiUrl);

        spAuthorisationProcedureUI = new JScrollPane(new JPanel());
        spAuthorisationProcedureUI.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spAuthorisationProcedureUI.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        spAuthorisationProcedureUI.getVerticalScrollBar().addComponentListener(
                new ComponentListener() {
                    public void componentShown(ComponentEvent e) {
                        spAuthorisationProcedureUI.setBorder(UIManager.getBorder("ScrollPane.border"));
                    }

                    public void componentHidden(ComponentEvent e) {
                        spAuthorisationProcedureUI.setBorder(null);
                    }

                    public void componentResized(ComponentEvent e) {}
                    public void componentMoved(ComponentEvent e) {}
                }
        );
        getContentPane().add(spAuthorisationProcedureUI, BorderLayout.CENTER);
        getContentPane().add(buildButtonRow(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHandler());
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getRootPane().getActionMap().put("cancel", new CancelAction());

        refreshAuthorisationProcedurePanel();

        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/OAuthAuthorisationWizard"));
    }

    /**
     * Creates the wizard.
     * 
     * @param apiUrl the API URL. Must not be null.
     * @throws IllegalArgumentException thrown if apiUrl is null
     */
    public OAuthAuthorisationWizard(String apiUrl) throws IllegalArgumentException {
        super(JOptionPane.getFrameForComponent(Main.parent),true /* modal */);
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        build();
        setApiUrl(apiUrl);
    }

    /**
     * Creates the wizard.
     * 
     * @param parent the component relative to which the dialog is displayed
     * @param apiUrl the API URL. Must not be null.
     * @throws IllegalArgumentException thrown if apiUrl is null
     */
    public OAuthAuthorisationWizard(Component parent, String apiUrl) {
        super(JOptionPane.getFrameForComponent(parent),true /* modal */);
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        build();
        setApiUrl(apiUrl);
    }

    /**
     * Sets the API URL for the API for which this wizard is generating
     * an Access Token.
     * 
     * @param apiUrl the API URL. Must not be null.
     * @throws IllegalArgumentException thrown if apiUrl is null
     */
    public void setApiUrl(String apiUrl) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(apiUrl, "apiUrl");
        this.apiUrl = apiUrl;
        setTitle(tr("Get an Access Token for ''{0}''", apiUrl));
        if (pnlFullyAutomaticAuthorisationUI != null) {
            pnlFullyAutomaticAuthorisationUI.setApiUrl(apiUrl);
        }
        if (pnlSemiAutomaticAuthorisationUI != null) {
            pnlSemiAutomaticAuthorisationUI.setApiUrl(apiUrl);
        }
        if (pnlManualAuthorisationUI != null) {
            pnlManualAuthorisationUI.setApiUrl(apiUrl);
        }

    }

    /**
     * Replies true if the dialog was cancelled
     * 
     * @return true if the dialog was cancelled
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected AbstractAuthorisationUI getCurrentAuthorisationUI() {
        switch((AuthorisationProcedure)cbAuthorisationProcedure.getSelectedItem()) {
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
        pnlFullyAutomaticAuthorisationUI.initFromPreferences(Main.pref);
        pnlSemiAutomaticAuthorisationUI.initFromPreferences(Main.pref);
        pnlManualAuthorisationUI.initFromPreferences(Main.pref);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400,400)
                    )
            ).apply(this);
            initFromPreferences();
        } else if (!visible && isShowing()){
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    class AuthorisationProcedureChangeListener implements ItemListener {
        public void itemStateChanged(ItemEvent arg0) {
            refreshAuthorisationProcedurePanel();
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and cancel authorisation"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
        }

        public void actionPerformed(ActionEvent evt) {
            cancel();
        }
    }

    class AcceptAccessTokenAction extends AbstractAction implements PropertyChangeListener {
        private OAuthToken token;

        public AcceptAccessTokenAction() {
            putValue(NAME, tr("Accept Access Token"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog and accept the Access Token"));
            updateEnabledState(null);
        }

        public void actionPerformed(ActionEvent evt) {
            setCanceled(false);
            setVisible(false);
        }

        public void updateEnabledState(OAuthToken token) {
            setEnabled(token != null);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(AbstractAuthorisationUI.ACCESS_TOKEN_PROP))
                return;
            token = (OAuthToken)evt.getNewValue();
            updateEnabledState(token);
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent arg0) {
            new CancelAction().cancel();
        }
    }
}
