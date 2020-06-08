// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
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
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

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
                + tr("JOSM will always listen at <b>port {0}</b> (http) on localhost."
                + "<br>This port is not configurable because it is referenced by external applications talking to JOSM.",
                Config.getPref().get("remote.control.port", "8111")) + "</html>");
        portLabel.setFont(portLabel.getFont().deriveFont(Font.PLAIN));
        remote.add(portLabel, GBC.eol().insets(5, 5, 0, 10).fill(GBC.HORIZONTAL));

        enableRemoteControl = new JCheckBox(tr("Enable remote control"), RemoteControl.PROP_REMOTECONTROL_ENABLED.get());
        remote.add(enableRemoteControl, GBC.eol());

        final JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray)));

        remote.add(wrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));

        wrapper.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL).insets(15, 5, 15, 5));

        wrapper.add(new JLabel(tr("Permitted actions:")), GBC.eol().insets(5, 0, 0, 0));
        for (JCheckBox p : prefs.values()) {
            wrapper.add(p, GBC.eol().insets(15, 5, 0, 0).fill(GBC.HORIZONTAL));
        }

        wrapper.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL).insets(15, 5, 15, 5));
        wrapper.add(loadInNewLayer, GBC.eol().fill(GBC.HORIZONTAL));
        wrapper.add(alwaysAskUserConfirm, GBC.eol().fill(GBC.HORIZONTAL));

        remote.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

        loadInNewLayer.setSelected(RequestHandler.LOAD_IN_NEW_LAYER.get());
        alwaysAskUserConfirm.setSelected(RequestHandler.GLOBAL_CONFIRMATION.get());

        ActionListener remoteControlEnabled = e -> GuiHelper.setEnabledRec(wrapper, enableRemoteControl.isSelected());
        enableRemoteControl.addActionListener(remoteControlEnabled);
        remoteControlEnabled.actionPerformed(null);
        createPreferenceTabWithScrollPane(gui, remote);
    }

    @Override
    public boolean ok() {
        boolean enabled = enableRemoteControl.isSelected();
        boolean changed = RemoteControl.PROP_REMOTECONTROL_ENABLED.put(enabled);
        if (enabled) {
            for (Entry<PermissionPrefWithDefault, JCheckBox> p : prefs.entrySet()) {
                Config.getPref().putBoolean(p.getKey().pref, p.getValue().isSelected());
            }
            RequestHandler.LOAD_IN_NEW_LAYER.put(loadInNewLayer.isSelected());
            RequestHandler.GLOBAL_CONFIRMATION.put(alwaysAskUserConfirm.isSelected());
        }
        if (changed) {
            if (enabled) {
                RemoteControl.start();
            } else {
                RemoteControl.stop();
            }
        }
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/RemoteControl");
    }
}
