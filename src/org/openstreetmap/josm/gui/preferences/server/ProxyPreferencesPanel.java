// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.Authenticator.RequestorType;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.ProxyPolicy;
import org.openstreetmap.josm.io.auth.CredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Component allowing input of proxy settings.
 */
public class ProxyPreferencesPanel extends VerticallyScrollablePanel {

    static final class AutoSizePanel extends JPanel {
        AutoSizePanel() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }

    private transient Map<ProxyPolicy, JRadioButton> rbProxyPolicy;
    private final JosmTextField tfProxyHttpHost = new JosmTextField();
    private final JosmTextField tfProxyHttpPort = new JosmTextField(5);
    private final JosmTextField tfProxySocksHost = new JosmTextField(20);
    private final JosmTextField tfProxySocksPort = new JosmTextField(5);
    private final JosmTextField tfProxyHttpUser = new JosmTextField(20);
    private final JosmPasswordField tfProxyHttpPassword = new JosmPasswordField(20);

    private JPanel pnlHttpProxyConfigurationPanel;
    private JPanel pnlSocksProxyConfigurationPanel;

    /**
     * Builds the panel for the HTTP proxy configuration
     *
     * @return panel with HTTP proxy configuration
     */
    protected final JPanel buildHttpProxyConfigurationPanel() {
        JPanel pnl = new AutoSizePanel();
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Host:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxyHttpHost, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        pnl.add(new JLabel(trc("server", "Port:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxyHttpPort, gc);
        tfProxyHttpPort.setMinimumSize(tfProxyHttpPort.getPreferredSize());

        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(new JMultilineLabel(tr("Please enter a username and a password if your proxy requires authentication.")), gc);

        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("User:")), gc);

        gc.gridy = 3;
        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxyHttpUser, gc);
        tfProxyHttpUser.setMinimumSize(tfProxyHttpUser.getPreferredSize());

        gc.gridy = 4;
        gc.gridx = 0;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Password:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxyHttpPassword, gc);
        tfProxyHttpPassword.setMinimumSize(tfProxyHttpPassword.getPreferredSize());

        // add an extra spacer, otherwise the layout is broken
        gc.gridy = 5;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    /**
     * Builds the panel for the SOCKS proxy configuration
     *
     * @return panel with SOCKS proxy configuration
     */
    protected final JPanel buildSocksProxyConfigurationPanel() {
        JPanel pnl = new AutoSizePanel();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Host:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxySocksHost, gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.NONE;
        pnl.add(new JLabel(trc("server", "Port:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfProxySocksPort, gc);
        tfProxySocksPort.setMinimumSize(tfProxySocksPort.getPreferredSize());

        // add an extra spacer, otherwise the layout is broken
        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    protected final JPanel buildProxySettingsPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        ButtonGroup bgProxyPolicy = new ButtonGroup();
        rbProxyPolicy = new EnumMap<>(ProxyPolicy.class);
        ProxyPolicyChangeListener policyChangeListener = new ProxyPolicyChangeListener();
        for (ProxyPolicy pp: ProxyPolicy.values()) {
            rbProxyPolicy.put(pp, new JRadioButton());
            bgProxyPolicy.add(rbProxyPolicy.get(pp));
            rbProxyPolicy.get(pp).addItemListener(policyChangeListener);
        }

        // radio button "No proxy"
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.NO_PROXY), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("No proxy")), gc);

        // radio button "System settings"
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_SYSTEM_SETTINGS), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        String msg;
        if (DefaultProxySelector.willJvmRetrieveSystemProxies()) {
            msg = tr("Use standard system settings");
        } else {
            msg = tr("Use standard system settings (disabled. Start JOSM with <tt>-Djava.net.useSystemProxies=true</tt> to enable)");
        }
        pnl.add(new JMultilineLabel("<html>" + msg + "</html>"), gc);

        // radio button http proxy
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0.0;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("Manually configure a HTTP proxy")), gc);

        // the panel with the http proxy configuration parameters
        gc.gridx = 1;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnlHttpProxyConfigurationPanel = buildHttpProxyConfigurationPanel();
        pnl.add(pnlHttpProxyConfigurationPanel, gc);

        // radio button SOCKS proxy
        gc.gridx = 0;
        gc.gridy = 4;
        gc.weightx = 0.0;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_SOCKS_PROXY), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("Use a SOCKS proxy")), gc);

        // the panel with the SOCKS configuration parameters
        gc.gridx = 1;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnlSocksProxyConfigurationPanel = buildSocksProxyConfigurationPanel();
        pnl.add(pnlSocksProxyConfigurationPanel, gc);

        return pnl;
    }

    /**
     * Initializes the panel with the values from the preferences
     */
    public final void initFromPreferences() {
        ProxyPolicy pp = Optional.ofNullable(ProxyPolicy.fromName(Config.getPref().get(DefaultProxySelector.PROXY_POLICY, null)))
                .orElse(ProxyPolicy.NO_PROXY);
        rbProxyPolicy.get(pp).setSelected(true);
        tfProxyHttpHost.setText(Config.getPref().get(DefaultProxySelector.PROXY_HTTP_HOST, ""));
        tfProxyHttpPort.setText(Config.getPref().get(DefaultProxySelector.PROXY_HTTP_PORT, ""));
        tfProxySocksHost.setText(Config.getPref().get(DefaultProxySelector.PROXY_SOCKS_HOST, ""));
        tfProxySocksPort.setText(Config.getPref().get(DefaultProxySelector.PROXY_SOCKS_PORT, ""));

        if (pp == ProxyPolicy.USE_SYSTEM_SETTINGS && !DefaultProxySelector.willJvmRetrieveSystemProxies()) {
            Logging.warn(tr("JOSM is configured to use proxies from the system setting, but the JVM is not configured to retrieve them. " +
                         "Resetting preferences to ''No proxy''"));
            pp = ProxyPolicy.NO_PROXY;
            rbProxyPolicy.get(pp).setSelected(true);
        }

        // save the proxy user and the proxy password to a credentials store managed by
        // the credentials manager
        CredentialsAgent cm = CredentialsManager.getInstance();
        try {
            PasswordAuthentication pa = cm.lookup(RequestorType.PROXY, tfProxyHttpHost.getText());
            if (pa == null) {
                tfProxyHttpUser.setText("");
                tfProxyHttpPassword.setText("");
            } else {
                tfProxyHttpUser.setText(pa.getUserName() == null ? "" : pa.getUserName());
                tfProxyHttpPassword.setText(pa.getPassword() == null ? "" : String.valueOf(pa.getPassword()));
            }
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            tfProxyHttpUser.setText("");
            tfProxyHttpPassword.setText("");
        }
    }

    protected final void updateEnabledState() {
        boolean isHttpProxy = rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY).isSelected();
        for (Component c: pnlHttpProxyConfigurationPanel.getComponents()) {
            c.setEnabled(isHttpProxy);
        }

        boolean isSocksProxy = rbProxyPolicy.get(ProxyPolicy.USE_SOCKS_PROXY).isSelected();
        for (Component c: pnlSocksProxyConfigurationPanel.getComponents()) {
            c.setEnabled(isSocksProxy);
        }

        rbProxyPolicy.get(ProxyPolicy.USE_SYSTEM_SETTINGS).setEnabled(DefaultProxySelector.willJvmRetrieveSystemProxies());
    }

    class ProxyPolicyChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }
    }

    /**
     * Constructs a new {@code ProxyPreferencesPanel}.
     */
    public ProxyPreferencesPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(buildProxySettingsPanel(), GBC.eop().anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.BOTH));

        initFromPreferences();
        updateEnabledState();

        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#ProxySettings"));
    }

    /**
     * Saves the current values to the preferences
     */
    public void saveToPreferences() {
        ProxyPolicy policy = Arrays.stream(ProxyPolicy.values())
                .filter(pp -> rbProxyPolicy.get(pp).isSelected())
                .findFirst().orElse(null);
        Config.getPref().put(DefaultProxySelector.PROXY_POLICY, Optional.ofNullable(policy).orElse(ProxyPolicy.NO_PROXY).getName());
        Config.getPref().put(DefaultProxySelector.PROXY_HTTP_HOST, tfProxyHttpHost.getText());
        Config.getPref().put(DefaultProxySelector.PROXY_HTTP_PORT, tfProxyHttpPort.getText());
        Config.getPref().put(DefaultProxySelector.PROXY_SOCKS_HOST, tfProxySocksHost.getText());
        Config.getPref().put(DefaultProxySelector.PROXY_SOCKS_PORT, tfProxySocksPort.getText());

        // update the proxy selector
        ProxySelector selector = ProxySelector.getDefault();
        if (selector instanceof DefaultProxySelector) {
            ((DefaultProxySelector) selector).initFromPreferences();
        }

        CredentialsAgent cm = CredentialsManager.getInstance();
        try {
            PasswordAuthentication pa = new PasswordAuthentication(
                    tfProxyHttpUser.getText().trim(),
                    tfProxyHttpPassword.getPassword()
            );
            cm.store(RequestorType.PROXY, tfProxyHttpHost.getText(), pa);
        } catch (CredentialsAgentException e) {
            Logging.error(e);
        }
    }
}
