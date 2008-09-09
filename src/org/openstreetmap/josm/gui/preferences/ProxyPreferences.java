// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class ProxyPreferences implements PreferenceSetting {

	public static final String PROXY_ENABLE = "proxy.enable";
	public static final String PROXY_HOST = "proxy.host";
	public static final String PROXY_PORT = "proxy.port";
	public static final String PROXY_ANONYMOUS = "proxy.anonymous";
	public static final String PROXY_USER = "proxy.user";
	public static final String PROXY_PASS = "proxy.pass";

	private JCheckBox proxyEnable = new JCheckBox(tr("Enable proxy server"));
	private JTextField proxyHost = new JTextField(50);
	private JTextField proxyPort = new JTextField(5);
	private JCheckBox proxyAnonymous = new JCheckBox(tr("Anonymous"));
	private JTextField proxyUser = new JTextField(50);
	private JPasswordField proxyPass = new JPasswordField(50);

	public void addGui(PreferenceDialog gui) {
		proxyEnable.setSelected(Main.pref.getBoolean(PROXY_ENABLE));
		proxyEnable.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				proxyHost.setEnabled(proxyEnable.isSelected());
				proxyPort.setEnabled(proxyEnable.isSelected());
				proxyAnonymous.setEnabled(proxyEnable.isSelected());
				proxyUser.setEnabled(proxyEnable.isSelected() && proxyAnonymous.isSelected());
				proxyPass.setEnabled(proxyEnable.isSelected() && proxyAnonymous.isSelected());
			}
		});
		proxyHost.setEnabled(Main.pref.getBoolean(PROXY_ENABLE));
		proxyHost.setText(Main.pref.get(PROXY_HOST));
		proxyPort.setEnabled(Main.pref.getBoolean(PROXY_ENABLE));
		proxyPort.setText(Main.pref.get(PROXY_PORT));
		proxyAnonymous.setSelected(Main.pref.getBoolean(PROXY_ANONYMOUS));
		proxyAnonymous.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				proxyUser.setEnabled(proxyEnable.isSelected() && proxyAnonymous.isSelected());
				proxyPass.setEnabled(proxyEnable.isSelected() && proxyAnonymous.isSelected());
			}
		});
		proxyUser.setEnabled(Main.pref.getBoolean(PROXY_ENABLE) && (Main.pref.getBoolean(PROXY_ANONYMOUS)));
		proxyUser.setText(Main.pref.get(PROXY_USER));
		proxyPass.setEnabled(Main.pref.getBoolean(PROXY_ENABLE) && (Main.pref.getBoolean(PROXY_ANONYMOUS)));
		proxyPass.setText(Main.pref.get(PROXY_USER));
		
		gui.connection.add(new JSeparator(SwingConstants.HORIZONTAL), GBC.eol().fill(GBC.HORIZONTAL));
		gui.connection.add(new JLabel(tr("Proxy Settings")), GBC.eol());
		gui.connection.add(proxyEnable, GBC.eol().insets(20, 0, 0, 0));
		gui.connection.add(new JLabel(tr("Proxy server host")), GBC.std());
		gui.connection.add(proxyHost, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		gui.connection.add(new JLabel(tr("Proxy server port")), GBC.std());
		gui.connection.add(proxyPort, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		gui.connection.add(proxyAnonymous, GBC.eop().insets(20, 0, 0, 0));
		gui.connection.add(new JLabel(tr("Proxy server username")), GBC.std());
		gui.connection.add(proxyUser, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		gui.connection.add(new JLabel(tr("Proxy server password")), GBC.std());
		gui.connection.add(proxyPass, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		gui.connection.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
	}

	public void ok() {
		Main.pref.put(PROXY_ENABLE, proxyEnable.isSelected());
		Main.pref.put(PROXY_HOST, proxyHost.getText());
		Main.pref.put(PROXY_PORT, proxyPort.getText());
		Main.pref.put(PROXY_ANONYMOUS, proxyAnonymous.isSelected());
		Main.pref.put(PROXY_USER, proxyUser.getText());
		Main.pref.put(PROXY_PASS, new String(proxyPass.getPassword()));
	}

}
