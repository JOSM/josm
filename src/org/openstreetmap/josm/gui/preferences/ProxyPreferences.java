// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.ProxySelector;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.JMultilineLabel;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.tools.GBC;

public class ProxyPreferences implements PreferenceSetting {



    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProxyPreferences();
        }
    }

    public enum ProxyPolicy {
        NO_PROXY("no-proxy"),
        USE_SYSTEM_SETTINGS("use-system-settings"),
        USE_HTTP_PROXY("use-http-proxy"),
        USE_SOCKS_PROXY("use-socks-proxy");

        private String policyName;
        ProxyPolicy(String policyName) {
            this.policyName = policyName;
        }

        public String getName() {
            return policyName;
        }

        static public ProxyPolicy fromName(String policyName) {
            if (policyName == null) return null;
            policyName = policyName.trim().toLowerCase();
            for(ProxyPolicy pp: values()) {
                if (pp.getName().equals(policyName))
                    return pp;
            }
            return null;
        }
    }

    public static final String PROXY_POLICY = "proxy.policy";
    public static final String PROXY_HTTP_HOST = "proxy.http.host";
    public static final String PROXY_HTTP_PORT = "proxy.http.port";
    public static final String PROXY_SOCKS_HOST = "proxy.socks.host";
    public static final String PROXY_SOCKS_PORT = "proxy.socks.port";
    public static final String PROXY_USER = "proxy.user";
    public static final String PROXY_PASS = "proxy.pass";

    private ButtonGroup bgProxyPolicy;
    private Map<ProxyPolicy, JRadioButton> rbProxyPolicy;
    private JTextField tfProxyHttpHost;
    private JTextField tfProxyHttpPort;
    private JTextField tfProxySocksHost;
    private JTextField tfProxySocksPort;
    private JTextField tfProxyHttpUser;
    private JPasswordField tfProxyHttpPassword;

    protected JPanel buildHttpProxyConfigurationPanel() {
        JPanel pnl = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5,5,0,0);
        pnl.add(new JLabel(tr("Host:")), gc);

        gc.gridx = 1;
        pnl.add(tfProxyHttpHost = new JTextField(20),gc);

        gc.gridy = 1;
        gc.gridx = 0;
        pnl.add(new JLabel(tr("Port:")), gc);

        gc.gridx = 1;
        gc.weightx = 0.0;
        pnl.add(tfProxyHttpPort = new JTextField(5),gc);

        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JMultilineLabel(tr("Please enter a username and a password if your proxy requires authentication.")), gc);

        gc.gridy = 3;
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.NONE;
        pnl.add(new JLabel(tr("User:")), gc);

        gc.gridy = 3;
        gc.gridx = 1;
        pnl.add(tfProxyHttpUser = new JTextField(20),gc);

        gc.gridy = 4;
        gc.gridx = 0;
        pnl.add(new JLabel(tr("Password:")), gc);

        gc.gridx = 1;
        pnl.add(tfProxyHttpPassword = new JPasswordField(20),gc);
        return pnl;
    }

    protected JPanel buildSocksProxyConfigurationPanel() {
        JPanel pnl = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5,5,0,0);
        pnl.add(new JLabel(tr("Host:")), gc);

        gc.gridx = 1;
        pnl.add(tfProxySocksHost = new JTextField(20),gc);

        gc.gridy = 1;
        gc.gridx = 0;
        pnl.add(new JLabel(tr("Port:")), gc);

        gc.gridx = 1;
        pnl.add(tfProxySocksPort = new JTextField(5),gc);

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

    protected JPanel buildProxySettingsPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        bgProxyPolicy = new ButtonGroup();
        rbProxyPolicy = new HashMap<ProxyPolicy, JRadioButton>();
        ProxyPolicyChangeListener policyChangeListener = new ProxyPolicyChangeListener();
        for (ProxyPolicy pp: ProxyPolicy.values()) {
            rbProxyPolicy.put(pp, new JRadioButton());
            bgProxyPolicy.add(rbProxyPolicy.get(pp));
            rbProxyPolicy.get(pp).addItemListener(policyChangeListener);
        }
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.NO_PROXY),gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("No proxy")), gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_SYSTEM_SETTINGS),gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        String msg;
        if (DefaultProxySelector.willJvmRetrieveSystemProxies()) {
            msg = tr("Use standard system settings");
        } else {
            msg = tr("Use standard system settings (disabled. Start JOSM with <tt>-Djava.net.useSystemProxies=true</tt> to enable)");
        }
        pnl.add(new JMultilineLabel("<html>" + msg + "</html>"), gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY),gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("Manually configure a HTTP proxy")),gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(buildHttpProxyConfigurationPanel(),gc);

        gc.gridx = 0;
        gc.gridy = 4;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(rbProxyPolicy.get(ProxyPolicy.USE_SOCKS_PROXY),gc);
        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        pnl.add(new JLabel(tr("Use a SOCKS proxy")),gc);

        gc.gridx = 1;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        pnl.add(buildSocksProxyConfigurationPanel(),gc);

        return pnl;
    }

    protected void initFromPreferences() {
        String policy = Main.pref.get(PROXY_POLICY, null);
        ProxyPolicy pp = ProxyPolicy.fromName(policy);
        pp = pp == null? ProxyPolicy.NO_PROXY: pp;
        rbProxyPolicy.get(pp).setSelected(true);
        String value = Main.pref.get("proxy.host", null);
        if (value != null) {
            // legacy support
            tfProxyHttpHost.setText(value);
            Main.pref.put("proxy.host", null);
        } else {
            tfProxyHttpHost.setText(Main.pref.get(PROXY_HTTP_HOST, ""));
        }
        value = Main.pref.get("proxy.port", null);
        if (value != null) {
            // legacy support
            tfProxyHttpPort.setText(value);
            Main.pref.put("proxy.port", null);
        } else {
            tfProxyHttpPort.setText(Main.pref.get(PROXY_HTTP_PORT, ""));
        }
        tfProxySocksHost.setText(Main.pref.get(PROXY_SOCKS_HOST, ""));
        tfProxySocksPort.setText(Main.pref.get(PROXY_SOCKS_PORT, ""));
        tfProxyHttpUser.setText(Main.pref.get(PROXY_USER, ""));
        tfProxyHttpPassword.setText(Main.pref.get(PROXY_PASS, ""));

        if (pp.equals(ProxyPolicy.USE_SYSTEM_SETTINGS) && ! DefaultProxySelector.willJvmRetrieveSystemProxies()) {
            System.err.println(tr("Warning: JOSM is configured to use proxies from the system setting, but the JVM is not configured to retrieve them. Resetting preferences to ''No proxy''"));
            pp = ProxyPolicy.NO_PROXY;
            rbProxyPolicy.get(pp).setSelected(true);
        }
    }

    protected void updateEnabledState() {
        tfProxyHttpHost.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY).isSelected());
        tfProxyHttpPort.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY).isSelected());
        tfProxyHttpUser.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY).isSelected());
        tfProxyHttpPassword.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_HTTP_PROXY).isSelected());
        tfProxySocksHost.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_SOCKS_PROXY).isSelected());
        tfProxySocksPort.setEnabled(rbProxyPolicy.get(ProxyPolicy.USE_SOCKS_PROXY).isSelected());

        rbProxyPolicy.get(ProxyPolicy.USE_SYSTEM_SETTINGS).setEnabled(DefaultProxySelector.willJvmRetrieveSystemProxies());
    }

    class ProxyPolicyChangeListener implements ItemListener {
        public void itemStateChanged(ItemEvent arg0) {
            updateEnabledState();
        }
    }

    public void addGui(PreferenceDialog gui) {
        gui.connection.add(new JSeparator(SwingConstants.HORIZONTAL), GBC.eol().fill(GBC.HORIZONTAL));
        gui.connection.add(new JLabel(tr("Proxy Settings")), GBC.eol());
        gui.connection.add(buildProxySettingsPanel(), GBC.eol().insets(20,10,0,0));

        initFromPreferences();
        updateEnabledState();
    }

    public boolean ok() {
        ProxyPolicy policy = null;
        for (ProxyPolicy pp: ProxyPolicy.values()) {
            if (rbProxyPolicy.get(pp).isSelected()) {
                policy = pp;
                break;
            }
        }
        if (policy == null) {
            policy = ProxyPolicy.NO_PROXY;
        }
        Main.pref.put(PROXY_POLICY, policy.getName());
        Main.pref.put(PROXY_HTTP_HOST, tfProxyHttpHost.getText());
        Main.pref.put(PROXY_HTTP_PORT, tfProxyHttpPort.getText());
        Main.pref.put(PROXY_SOCKS_HOST, tfProxySocksHost.getText());
        Main.pref.put(PROXY_SOCKS_PORT, tfProxySocksPort.getText());
        Main.pref.put(PROXY_USER, tfProxyHttpUser.getText());
        Main.pref.put(PROXY_PASS, String.valueOf(tfProxyHttpPassword.getPassword()));

        // remove these legacy property keys
        Main.pref.put("proxy.anonymous", null);
        Main.pref.put("proxy.enable", null);

        // update the proxy selector
        ProxySelector selector = ProxySelector.getDefault();
        if (selector instanceof DefaultProxySelector) {
            ((DefaultProxySelector)selector).initFromPreferences();
        }
        return false;
    }
}
