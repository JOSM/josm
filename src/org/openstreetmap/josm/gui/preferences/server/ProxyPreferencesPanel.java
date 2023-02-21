// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static java.awt.GridBagConstraints.HORIZONTAL;
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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.gui.widgets.EditableList;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.ProxyPolicy;
import org.openstreetmap.josm.io.auth.CredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Component allowing input of proxy settings.
 */
public class ProxyPreferencesPanel extends VerticallyScrollablePanel {

    private static final long serialVersionUID = -2479852374189976764L;

    static final class AutoSizePanel extends JPanel {
        private static final long serialVersionUID = 7469560761925020366L;

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
    private final EditableList tfExceptionHosts = new EditableList(tr("No proxy for"));
    private final EditableList tfIncludeHosts = new EditableList(tr("Proxy only for"));

    private JPanel pnlHttpProxyConfigurationPanel;
    private JPanel pnlSocksProxyConfigurationPanel;
    private JPanel pnlIncludeOrExcludeHostsPanel;

    /**
     * Builds the panel for the HTTP proxy configuration
     *
     * @return panel with HTTP proxy configuration
     */
    protected final JPanel buildHttpProxyConfigurationPanel() {
        JPanel pnl = new AutoSizePanel();
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.fill = HORIZONTAL;
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
        gc.fill = HORIZONTAL;
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
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.fill = HORIZONTAL;
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

    protected final JPanel buildExceptionIncludesHostsProxyConfigurationPanel() {
        JPanel pnl = new AutoSizePanel();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(5, 5, 0, 0);
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("No proxy for (hosts):")), gc);

        gc.gridx = 1;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.NONE;
        pnl.add(new JLabel(tr("Proxy only for (hosts):")), gc);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.fill = HORIZONTAL;
        tfExceptionHosts.setMinimumSize(tfExceptionHosts.getPreferredSize());
        pnl.add(tfExceptionHosts, gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.fill = HORIZONTAL;
        tfIncludeHosts.setMinimumSize(tfIncludeHosts.getPreferredSize());
        pnl.add(tfIncludeHosts, gc);

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

        ButtonGroup bgProxyPolicy = new ButtonGroup();
        rbProxyPolicy = new EnumMap<>(ProxyPolicy.class);
        ProxyPolicyChangeListener policyChangeListener = new ProxyPolicyChangeListener();
        for (ProxyPolicy pp: ProxyPolicy.values()) {
            rbProxyPolicy.put(pp, new JRadioButton());
            bgProxyPolicy.add(rbProxyPolicy.get(pp));
            rbProxyPolicy.get(pp).addItemListener(policyChangeListener);
        }

        // radio button "No proxy"
        pnl.add(newRadioButton(ProxyPolicy.NO_PROXY, tr("No proxy")), GBC.eop().anchor(GBC.NORTHWEST));

        // radio button "System settings"
        pnl.add(newRadioButton(ProxyPolicy.USE_SYSTEM_SETTINGS, tr("Use standard system settings")), GBC.eol());
        if (!DefaultProxySelector.willJvmRetrieveSystemProxies()) {
            String msg = tr("Use standard system settings (disabled. Start JOSM with <tt>-Djava.net.useSystemProxies=true</tt> to enable)");
            pnl.add(new JMultilineLabel("<html>" + msg + "</html>"), GBC.eop().fill(HORIZONTAL));
        }

        // radio button http proxy
        pnl.add(newRadioButton(ProxyPolicy.USE_HTTP_PROXY, tr("Manually configure a HTTP proxy")), GBC.eol());

        // the panel with the http proxy configuration parameters
        pnlHttpProxyConfigurationPanel = buildHttpProxyConfigurationPanel();
        pnl.add(pnlHttpProxyConfigurationPanel, GBC.eop().fill(HORIZONTAL));

        // radio button SOCKS proxy
        pnl.add(newRadioButton(ProxyPolicy.USE_SOCKS_PROXY, tr("Use a SOCKS proxy")), GBC.eol());

        // the panel with the SOCKS configuration parameters
        pnlSocksProxyConfigurationPanel = buildSocksProxyConfigurationPanel();
        pnl.add(pnlSocksProxyConfigurationPanel, GBC.eop().fill(HORIZONTAL));

        pnl.add(Box.createVerticalGlue(), GBC.eol().fill());

        // the panel with the exception and includes hosts
        pnlIncludeOrExcludeHostsPanel = buildExceptionIncludesHostsProxyConfigurationPanel();
        pnl.add(pnlIncludeOrExcludeHostsPanel, GBC.eop().fill(HORIZONTAL));

        pnl.add(Box.createVerticalGlue(), GBC.eol().fill());

        return pnl;
    }

    private JRadioButton newRadioButton(ProxyPolicy policy, String text) {
        JRadioButton radioButton = rbProxyPolicy.get(policy);
        radioButton.setText(text);
        return radioButton;
    }

    /**
     * Initializes the panel with the values from the preferences
     */
    public final void initFromPreferences() {
        IPreferences pref = Config.getPref();
        ProxyPolicy pp = Optional.ofNullable(ProxyPolicy.fromName(pref.get(DefaultProxySelector.PROXY_POLICY, null)))
                .orElse(ProxyPolicy.NO_PROXY);
        rbProxyPolicy.get(pp).setSelected(true);
        tfProxyHttpHost.setText(pref.get(DefaultProxySelector.PROXY_HTTP_HOST, ""));
        tfProxyHttpPort.setText(pref.get(DefaultProxySelector.PROXY_HTTP_PORT, ""));
        tfProxySocksHost.setText(pref.get(DefaultProxySelector.PROXY_SOCKS_HOST, ""));
        tfProxySocksPort.setText(pref.get(DefaultProxySelector.PROXY_SOCKS_PORT, ""));
        tfExceptionHosts.setItems(pref.getList(DefaultProxySelector.PROXY_EXCEPTIONS, new ArrayList<>()));
        tfIncludeHosts.setItems(pref.getList(DefaultProxySelector.PROXY_INCLUDES, new ArrayList<>()));

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

        boolean proxyEnabled = !rbProxyPolicy.get(ProxyPolicy.NO_PROXY).isSelected();
        for (Component c : pnlIncludeOrExcludeHostsPanel.getComponents()) {
            c.setEnabled(proxyEnabled);
        }
    }

    class ProxyPolicyChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
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
    }

    /**
     * Saves the current values to the preferences
     */
    public void saveToPreferences() {
        ProxyPolicy policy = Arrays.stream(ProxyPolicy.values())
                .filter(pp -> rbProxyPolicy.get(pp).isSelected())
                .findFirst().orElse(null);
        IPreferences pref = Config.getPref();
        pref.put(DefaultProxySelector.PROXY_POLICY, Optional.ofNullable(policy).orElse(ProxyPolicy.NO_PROXY).getName());
        pref.put(DefaultProxySelector.PROXY_HTTP_HOST, tfProxyHttpHost.getText());
        pref.put(DefaultProxySelector.PROXY_HTTP_PORT, tfProxyHttpPort.getText());
        pref.put(DefaultProxySelector.PROXY_SOCKS_HOST, tfProxySocksHost.getText());
        pref.put(DefaultProxySelector.PROXY_SOCKS_PORT, tfProxySocksPort.getText());
        pref.putList(DefaultProxySelector.PROXY_EXCEPTIONS, tfExceptionHosts.getItems());
        pref.putList(DefaultProxySelector.PROXY_INCLUDES, tfIncludeHosts.getItems());


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
