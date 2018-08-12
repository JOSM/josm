// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.io.remotecontrol.RemoteControlHttpsServer;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.PlatformManager;

/**
 * Preference settings for Remote Control.
 *
 * @author Frederik Ramm
 */
public final class RemoteControlPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to build a new instance of this preference setting
     */
    public static class Factory implements PreferenceSettingFactory {

        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new RemoteControlPreference();
        }
    }

    private RemoteControlPreference() {
        super(/* ICON(preferences/) */ "remotecontrol", tr("Remote Control"), tr("Settings for the remote control feature."));
        for (PermissionPrefWithDefault p : PermissionPrefWithDefault.getPermissionPrefs()) {
            JCheckBox cb = new JCheckBox(p.preferenceText);
            cb.setSelected(p.isAllowed());
            prefs.put(p, cb);
        }
    }

    private final Map<PermissionPrefWithDefault, JCheckBox> prefs = new LinkedHashMap<>();
    private JCheckBox enableRemoteControl;
    private JCheckBox enableHttpsSupport;

    private JButton installCertificate;
    private JButton uninstallCertificate;

    private final JCheckBox loadInNewLayer = new JCheckBox(tr("Download as new layer"));
    private final JCheckBox alwaysAskUserConfirm = new JCheckBox(tr("Confirm all Remote Control actions manually"));

    @Override
    public void addGui(final PreferenceTabbedPane gui) {

        JPanel remote = new VerticallyScrollablePanel(new GridBagLayout());

        final JLabel descLabel = new JLabel("<html>"
                + tr("Allows JOSM to be controlled from other applications, e.g. from a web browser.")
                + "</html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN));
        remote.add(descLabel, GBC.eol().insets(5, 5, 0, 10).fill(GBC.HORIZONTAL));

        final JLabel portLabel = new JLabel("<html>"
                + tr("JOSM will always listen at <b>port {0}</b> (http) and <b>port {1}</b> (https) on localhost."
                + "<br>These ports are not configurable because they are referenced by external applications talking to JOSM.",
                Config.getPref().get("remote.control.port", "8111"),
                Config.getPref().get("remote.control.https.port", "8112")) + "</html>");
        portLabel.setFont(portLabel.getFont().deriveFont(Font.PLAIN));
        remote.add(portLabel, GBC.eol().insets(5, 5, 0, 10).fill(GBC.HORIZONTAL));

        enableRemoteControl = new JCheckBox(tr("Enable remote control"), RemoteControl.PROP_REMOTECONTROL_ENABLED.get());
        remote.add(enableRemoteControl, GBC.eol());

        final JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray)));

        remote.add(wrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));

        boolean https = RemoteControl.PROP_REMOTECONTROL_HTTPS_ENABLED.get();

        enableHttpsSupport = new JCheckBox(tr("Enable HTTPS support"), https);
        wrapper.add(enableHttpsSupport, GBC.eol().fill(GBC.HORIZONTAL));

        // Certificate installation only available on Windows for now, see #10033
        if (PlatformManager.isPlatformWindows()) {
            installCertificate = new JButton(tr("Install..."));
            uninstallCertificate = new JButton(tr("Uninstall..."));
            installCertificate.setToolTipText(tr("Install JOSM localhost certificate to system/browser root keystores"));
            uninstallCertificate.setToolTipText(tr("Uninstall JOSM localhost certificate from system/browser root keystores"));
            wrapper.add(new JLabel(tr("Certificate:")), GBC.std().insets(15, 5, 0, 0));
            wrapper.add(installCertificate, GBC.std().insets(5, 5, 0, 0));
            wrapper.add(uninstallCertificate, GBC.eol().insets(5, 5, 0, 0));
            enableHttpsSupport.addActionListener(e -> installCertificate.setEnabled(enableHttpsSupport.isSelected()));
            installCertificate.addActionListener(e -> {
                try {
                    boolean changed = RemoteControlHttpsServer.setupPlatform(
                            RemoteControlHttpsServer.loadJosmKeystore());
                    String msg = changed ?
                            tr("Certificate has been successfully installed.") :
                            tr("Certificate is already installed. Nothing to do.");
                    Logging.info(msg);
                    JOptionPane.showMessageDialog(wrapper, msg);
                } catch (IOException | GeneralSecurityException ex) {
                    Logging.error(ex);
                }
            });
            uninstallCertificate.addActionListener(e -> {
                try {
                    String msg;
                    KeyStore ks = PlatformHookWindows.getRootKeystore();
                    if (ks.containsAlias(RemoteControlHttpsServer.ENTRY_ALIAS)) {
                        Logging.info(tr("Removing certificate {0} from root keystore.", RemoteControlHttpsServer.ENTRY_ALIAS));
                        ks.deleteEntry(RemoteControlHttpsServer.ENTRY_ALIAS);
                        msg = tr("Certificate has been successfully uninstalled.");
                    } else {
                        msg = tr("Certificate is not installed. Nothing to do.");
                    }
                    Logging.info(msg);
                    JOptionPane.showMessageDialog(wrapper, msg);
                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
                    Logging.error(ex);
                }
            });
            installCertificate.setEnabled(https);
        }

        wrapper.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL).insets(15, 5, 15, 5));

        wrapper.add(new JLabel(tr("Permitted actions:")), GBC.eol().insets(5, 0, 0, 0));
        for (JCheckBox p : prefs.values()) {
            wrapper.add(p, GBC.eol().insets(15, 5, 0, 0).fill(GBC.HORIZONTAL));
        }

        wrapper.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL).insets(15, 5, 15, 5));
        wrapper.add(loadInNewLayer, GBC.eol().fill(GBC.HORIZONTAL));
        wrapper.add(alwaysAskUserConfirm, GBC.eol().fill(GBC.HORIZONTAL));

        remote.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

        loadInNewLayer.setSelected(Config.getPref().getBoolean(
                RequestHandler.loadInNewLayerKey, RequestHandler.loadInNewLayerDefault));
        alwaysAskUserConfirm.setSelected(Config.getPref().getBoolean(
                RequestHandler.globalConfirmationKey, RequestHandler.globalConfirmationDefault));

        ActionListener remoteControlEnabled = e -> {
            GuiHelper.setEnabledRec(wrapper, enableRemoteControl.isSelected());
            enableHttpsSupport.setEnabled(RemoteControl.supportsHttps());
            // 'setEnabled(false)' does not work for JLabel with html text, so do it manually
            // FIXME: use QuadStateCheckBox to make checkboxes unset when disabled
            if (installCertificate != null && uninstallCertificate != null) {
                // Install certificate button is enabled if HTTPS is also enabled
                installCertificate.setEnabled(enableRemoteControl.isSelected()
                        && enableHttpsSupport.isSelected() && RemoteControl.supportsHttps());
                // Uninstall certificate button is always enabled
                uninstallCertificate.setEnabled(RemoteControl.supportsHttps());
            }
        };
        enableRemoteControl.addActionListener(remoteControlEnabled);
        remoteControlEnabled.actionPerformed(null);
        createPreferenceTabWithScrollPane(gui, remote);
    }

    @Override
    public boolean ok() {
        boolean enabled = enableRemoteControl.isSelected();
        boolean httpsEnabled = enableHttpsSupport.isSelected();
        boolean changed = RemoteControl.PROP_REMOTECONTROL_ENABLED.put(enabled);
        boolean httpsChanged = RemoteControl.PROP_REMOTECONTROL_HTTPS_ENABLED.put(httpsEnabled);
        if (enabled) {
            for (Entry<PermissionPrefWithDefault, JCheckBox> p : prefs.entrySet()) {
                Config.getPref().putBoolean(p.getKey().pref, p.getValue().isSelected());
            }
            Config.getPref().putBoolean(RequestHandler.loadInNewLayerKey, loadInNewLayer.isSelected());
            Config.getPref().putBoolean(RequestHandler.globalConfirmationKey, alwaysAskUserConfirm.isSelected());
        }
        if (changed) {
            if (enabled) {
                RemoteControl.start();
            } else {
                RemoteControl.stop();
            }
        } else if (httpsChanged) {
            if (httpsEnabled) {
                RemoteControlHttpsServer.restartRemoteControlHttpsServer();
            } else {
                RemoteControlHttpsServer.stopRemoteControlHttpsServer();
            }
        }
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/RemoteControl");
    }
}
