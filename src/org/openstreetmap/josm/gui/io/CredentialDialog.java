// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreferencesPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class CredentialDialog extends JDialog {

    public static CredentialDialog getOsmApiCredentialDialog(String username, String password, String host, String saveUsernameAndPasswordCheckboxText) {
        CredentialDialog dialog = new CredentialDialog(saveUsernameAndPasswordCheckboxText);
        if (Objects.equals(OsmApi.getOsmApi().getHost(), host)) {
            dialog.prepareForOsmApiCredentials(username, password);
        } else {
            dialog.prepareForOtherHostCredentials(username, password, host);
        }
        dialog.pack();
        return dialog;
    }

    public static CredentialDialog getHttpProxyCredentialDialog(String username, String password, String host, String saveUsernameAndPasswordCheckboxText) {
        CredentialDialog dialog = new CredentialDialog(saveUsernameAndPasswordCheckboxText);
        dialog.prepareForProxyCredentials(username, password);
        dialog.pack();
        return dialog;
    }

    private boolean canceled;
    protected CredentialPanel pnlCredentials;
    private String saveUsernameAndPasswordCheckboxText;

    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            WindowGeometry.centerInWindow(Main.parent, new Dimension(350,300)).applySafe(this);
        }
        super.setVisible(visible);
    }

    protected JPanel createButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout());
        pnl.add(new SideButton(new OKAction()));
        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/Password"))));
        return pnl;
    }

    protected void build() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        addWindowListener(new WindowEventHander());
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        getRootPane().getActionMap().put("escape", new CancelAction());

        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    }

    public CredentialDialog(String saveUsernameAndPasswordCheckboxText) {
        this.saveUsernameAndPasswordCheckboxText = saveUsernameAndPasswordCheckboxText;
        setModalityType(ModalityType.DOCUMENT_MODAL);
        try {
            setAlwaysOnTop(true);
        } catch(SecurityException e) {
            Main.warn(tr("Failed to put Credential Dialog always on top. Caught security exception."));
        }
        build();
    }

    public void prepareForOsmApiCredentials(String username, String password) {
        setTitle(tr("Enter credentials for OSM API"));
        getContentPane().add(pnlCredentials = new OsmApiCredentialsPanel(this), BorderLayout.CENTER);
        pnlCredentials.init(username, password);
        validate();
    }

    public void prepareForOtherHostCredentials(String username, String password, String host) {
        setTitle(tr("Enter credentials for host"));
        getContentPane().add(pnlCredentials = new OtherHostCredentialsPanel(this, host), BorderLayout.CENTER);
        pnlCredentials.init(username, password);
        validate();
    }

    public void prepareForProxyCredentials(String username, String password) {
        setTitle(tr("Enter credentials for HTTP proxy"));
        getContentPane().add(pnlCredentials = new HttpProxyCredentialsPanel(this), BorderLayout.CENTER);
        pnlCredentials.init(username, password);
        validate();
    }

    public String getUsername() {
        if (pnlCredentials== null) return null;
        return pnlCredentials.getUserName();
    }

    public char[] getPassword() {
        if (pnlCredentials== null) return null;
        return pnlCredentials.getPassword();
    }

    public boolean isSaveCredentials() {
        if (pnlCredentials== null) return false;
        return pnlCredentials.isSaveCredentials();
    }

    protected static class CredentialPanel extends JPanel {
        protected JosmTextField tfUserName;
        protected JosmPasswordField tfPassword;
        protected JCheckBox cbSaveCredentials;
        protected JMultilineLabel lblHeading;
        protected JMultilineLabel lblWarning;
        protected CredentialDialog owner; // owner Dependency Injection to use Key listeners for username and password text fields

        protected void build() {
            tfUserName = new JosmTextField(20);
            tfPassword = new JosmPasswordField(20);
            tfUserName.addFocusListener(new SelectAllOnFocusHandler());
            tfPassword.addFocusListener(new SelectAllOnFocusHandler());
            tfUserName.addKeyListener(new TFKeyListener(owner, tfUserName, tfPassword));
            tfPassword.addKeyListener(new TFKeyListener(owner, tfPassword, tfUserName));
            cbSaveCredentials =  new JCheckBox(owner.saveUsernameAndPasswordCheckboxText);

            setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridwidth = 2;
            gc.gridheight = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(0,0,10,0);
            add(lblHeading = new JMultilineLabel(""), gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.gridwidth = 1;
            gc.gridheight = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.weighty = 0.0;
            gc.insets = new Insets(0,0,10,10);
            add(new JLabel(tr("Username")), gc);
            gc.gridx = 1;
            gc.gridy = 1;
            gc.weightx = 1.0;
            add(tfUserName, gc);
            gc.gridx = 0;
            gc.gridy = 2;
            gc.weightx = 0.0;
            add(new JLabel(tr("Password")), gc);

            gc.gridx = 1;
            gc.gridy = 2;
            gc.weightx = 0.0;
            add(tfPassword, gc);

            gc.gridx = 0;
            gc.gridy = 3;
            gc.gridwidth = 2;
            gc.gridheight = 1;
            gc.fill = GridBagConstraints.BOTH;
            gc.weightx = 1.0;
            gc.weighty = 0.0;
            lblWarning = new JMultilineLabel("");
            lblWarning.setFont(lblWarning.getFont().deriveFont(Font.ITALIC));
            add(lblWarning, gc);

            gc.gridx = 0;
            gc.gridy = 4;
            gc.weighty = 0.0;
            add(cbSaveCredentials, gc);

            // consume the remaining space
            gc.gridx = 0;
            gc.gridy = 5;
            gc.weighty = 1.0;
            add(new JPanel(),gc);

        }

        public CredentialPanel(CredentialDialog owner) {
            this.owner = owner;
        }

        public void init(String username, String password) {
            username = username == null ? "" : username;
            password = password == null ? "" : password;
            tfUserName.setText(username);
            tfPassword.setText(password);
            cbSaveCredentials.setSelected(!username.isEmpty() && !password.isEmpty());
        }

        public void startUserInput() {
            tfUserName.requestFocusInWindow();
        }

        public String getUserName() {
            return tfUserName.getText();
        }

        public char[] getPassword() {
            return tfPassword.getPassword();
        }

        public boolean isSaveCredentials() {
            return cbSaveCredentials.isSelected();
        }

        protected final void updateWarningLabel(String url) {
            boolean https = url != null && url.startsWith("https");
            if (https) {
                lblWarning.setText(null);
            } else {
                lblWarning.setText(tr("Warning: The password is transferred unencrypted."));
            }
            lblWarning.setVisible(!https);
        }
    }

    private static class OsmApiCredentialsPanel extends CredentialPanel {

        @Override
        protected void build() {
            super.build();
            tfUserName.setToolTipText(tr("Please enter the user name of your OSM account"));
            tfPassword.setToolTipText(tr("Please enter the password of your OSM account"));
            String apiUrl = OsmApi.getOsmApi().getBaseUrl();
            lblHeading.setText(
                    "<html>" + tr("Authenticating at the OSM API ''{0}'' failed. Please enter a valid username and a valid password.",
                            apiUrl) + "</html>");
            updateWarningLabel(apiUrl);
        }

        public OsmApiCredentialsPanel(CredentialDialog owner) {
            super(owner);
            build();
        }
    }

    private static class OtherHostCredentialsPanel extends CredentialPanel {

        private String host;

        @Override
        protected void build() {
            super.build();
            tfUserName.setToolTipText(tr("Please enter the user name of your account"));
            tfPassword.setToolTipText(tr("Please enter the password of your account"));
            lblHeading.setText(
                    "<html>" + tr("Authenticating at the host ''{0}'' failed. Please enter a valid username and a valid password.",
                            host) + "</html>");
            updateWarningLabel(host);
        }

        public OtherHostCredentialsPanel(CredentialDialog owner, String host) {
            super(owner);
            this.host = host;
            build();
        }
    }

    private static class HttpProxyCredentialsPanel extends CredentialPanel {
        @Override
        protected void build() {
            super.build();
            tfUserName.setToolTipText(tr("Please enter the user name for authenticating at your proxy server"));
            tfPassword.setToolTipText(tr("Please enter the password for authenticating at your proxy server"));
            lblHeading.setText(
                    "<html>" + tr("Authenticating at the HTTP proxy ''{0}'' failed. Please enter a valid username and a valid password.",
                            Main.pref.get(ProxyPreferencesPanel.PROXY_HTTP_HOST) + ":" + Main.pref.get(ProxyPreferencesPanel.PROXY_HTTP_PORT)) + "</html>");
            lblWarning.setText("<html>" + tr("Warning: depending on the authentication method the proxy server uses the password may be transferred unencrypted.") + "</html>");
        }

        public HttpProxyCredentialsPanel(CredentialDialog owner) {
            super(owner);
            build();
        }
    }

    private static class SelectAllOnFocusHandler extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            if (e.getSource() instanceof JTextField) {
                JTextField tf = (JTextField)e.getSource();
                tf.selectAll();
            }
        }
    }

    /**
     * Listener for username and password text fields key events.
     * When user presses Enter:
     *   If current text field is empty (or just contains a sequence of spaces), nothing happens (or all spaces become selected).
     *   If current text field is not empty, but the next one is (or just contains a sequence of spaces), focuses the next text field.
     *   If both text fields contain characters, submits the form by calling owner's {@link OKAction}.
     */
    private static class TFKeyListener implements KeyListener{
        protected CredentialDialog owner; // owner Dependency Injection to call OKAction
        protected JTextField currentTF;
        protected JTextField nextTF;

        public TFKeyListener (CredentialDialog owner, JTextField currentTF, JTextField nextTF) {
            this.owner = owner;
            this.currentTF = currentTF;
            this.nextTF = nextTF;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyChar() == KeyEvent.VK_ENTER) {
                if (currentTF.getText().trim().isEmpty()) {
                    currentTF.selectAll();
                    return;
                } else if (nextTF.getText().trim().isEmpty()) {
                    nextTF.requestFocusInWindow();
                    nextTF.selectAll();
                    return;
                } else {
                    OKAction okAction = owner.new OKAction();
                    okAction.actionPerformed(null);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped ( KeyEvent e ){
        }
    }

    class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, tr("Authenticate"));
            putValue(SHORT_DESCRIPTION, tr("Authenticate with the supplied username and password"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setCanceled(false);
            setVisible(false);
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SHORT_DESCRIPTION, tr("Cancel authentication"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
        }

        public void cancel() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            cancel();
        }
    }

    class WindowEventHander extends WindowAdapter {

        @Override
        public void windowActivated(WindowEvent e) {
            if (pnlCredentials != null) {
                pnlCredentials.startUserInput();
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            new CancelAction().cancel();
        }
    }
}
