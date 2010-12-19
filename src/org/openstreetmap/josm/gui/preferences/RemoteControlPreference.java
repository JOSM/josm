// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.io.remotecontrol.handler.AddNodeHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImageryHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.ImportHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.LoadAndZoomHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.io.remotecontrol.handler.VersionHandler;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preference settings for the Remote Control plugin
 *
 * @author Frederik Ramm
 */
public class RemoteControlPreference implements PreferenceSetting
{
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new RemoteControlPreference();
        }
    }

    private JCheckBox enableRemoteControl;

    private JCheckBox permissionLoadData = new JCheckBox(tr("Load data from API"));
    private JCheckBox permissionImportData = new JCheckBox(tr("Import data from URL"));
    private JCheckBox permissionLoadImagery = new JCheckBox(tr("Load imagery layers"));
    private JCheckBox permissionCreateObjects = new JCheckBox(tr("Create new objects"));
    private JCheckBox permissionChangeSelection = new JCheckBox(tr("Change the selection"));
    private JCheckBox permissionChangeViewport = new JCheckBox(tr("Change the viewport"));
    private JCheckBox permissionReadProtocolversion = new JCheckBox(tr("Read protocol version"));
    private JCheckBox alwaysAskUserConfirm = new JCheckBox(tr("Confirm all Remote Control actions manually"));

    public void addGui(final PreferenceTabbedPane gui) {

        JPanel remote = gui.createPreferenceTab("remotecontrol.gif", tr("Remote Control"), tr("Settings for the remote control feature."));

        remote.add(enableRemoteControl = new JCheckBox(tr("Enable remote control"), RemoteControl.PROP_REMOTECONTROL_ENABLED.get()), GBC.eol());

        final JPanel wrapper = new JPanel();
        wrapper.setLayout(new GridBagLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray)));

        remote.add(wrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));

        final JLabel descLabel = new JLabel("<html>"+
                tr("The remote control feature allows JOSM to be controlled from other applications, e.g. from a web browser.")
                + "</html>");
        wrapper.add(descLabel, GBC.eol().insets(5,5,0,10).fill(GBC.HORIZONTAL));
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN));

        wrapper.add(new JLabel(tr("Permitted actions:")), GBC.eol());
        int INDENT = 15;
        wrapper.add(permissionLoadData, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionImportData, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionLoadImagery, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionChangeSelection, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionChangeViewport, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionCreateObjects, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));
        wrapper.add(permissionReadProtocolversion, GBC.eol().insets(INDENT,5,0,0).fill(GBC.HORIZONTAL));

        wrapper.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL).insets(15, 5, 15, 5));

        wrapper.add(alwaysAskUserConfirm, GBC.eol().fill(GBC.HORIZONTAL));

        final JLabel portLabel = new JLabel("<html>"+tr("JOSM will always listen at port 8111 on localhost." +
                "This port is not configurable because it is referenced by external applications talking to JOSM.") + "</html>");
        portLabel.setFont(portLabel.getFont().deriveFont(Font.PLAIN));

        wrapper.add(portLabel, GBC.eol().insets(5,5,0,10).fill(GBC.HORIZONTAL));

        remote.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

        permissionLoadData.setSelected(Main.pref.getBoolean(LoadAndZoomHandler.loadDataPermissionKey, LoadAndZoomHandler.loadDataPermissionDefault));
        permissionImportData.setSelected(Main.pref.getBoolean(ImportHandler.permissionKey, ImportHandler.permissionDefault));
        permissionLoadImagery.setSelected(Main.pref.getBoolean(ImageryHandler.permissionKey, ImageryHandler.permissionDefault));
        permissionChangeSelection.setSelected(Main.pref.getBoolean(LoadAndZoomHandler.changeSelectionPermissionKey, LoadAndZoomHandler.changeSelectionPermissionDefault));
        permissionChangeViewport.setSelected(Main.pref.getBoolean(LoadAndZoomHandler.changeViewportPermissionKey, LoadAndZoomHandler.changeViewportPermissionDefault));
        permissionCreateObjects.setSelected(Main.pref.getBoolean(AddNodeHandler.permissionKey, AddNodeHandler.permissionDefault));
        permissionReadProtocolversion.setSelected(Main.pref.getBoolean(VersionHandler.permissionKey, VersionHandler.permissionDefault));
        alwaysAskUserConfirm.setSelected(Main.pref.getBoolean(RequestHandler.globalConfirmationKey, RequestHandler.globalConfirmationDefault));

        ActionListener remoteControlEnabled = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean enabled = enableRemoteControl.isSelected();
                GuiHelper.setEnabledRec(wrapper, enableRemoteControl.isSelected());
                // 'setEnabled(false)' does not work for JLabel with html text, so do it manually
                portLabel.setForeground(enabled ? UIManager.getColor("Label.foreground") : UIManager.getColor("Label.disabledForeground"));
                descLabel.setForeground(enabled ? UIManager.getColor("Label.foreground") : UIManager.getColor("Label.disabledForeground"));
                // FIXME: use QuadStateCheckBox to make checkboxes unset when disabled
            }
        };
        enableRemoteControl.addActionListener(remoteControlEnabled);
        remoteControlEnabled.actionPerformed(null);
    }

    public boolean ok() {
        boolean enabled = enableRemoteControl.isSelected();
        boolean changed = RemoteControl.PROP_REMOTECONTROL_ENABLED.put(enabled);
        if (enabled) {
            Main.pref.put(LoadAndZoomHandler.loadDataPermissionKey, permissionLoadData.isSelected());
            Main.pref.put(ImportHandler.permissionKey, permissionImportData.isSelected());
            Main.pref.put(ImageryHandler.permissionKey, permissionLoadImagery.isSelected());
            Main.pref.put(LoadAndZoomHandler.changeSelectionPermissionKey, permissionChangeSelection.isSelected());
            Main.pref.put(LoadAndZoomHandler.changeViewportPermissionKey, permissionChangeViewport.isSelected());
            Main.pref.put(AddNodeHandler.permissionKey, permissionCreateObjects.isSelected());
            Main.pref.put(VersionHandler.permissionKey, permissionReadProtocolversion.isSelected());
            Main.pref.put(RequestHandler.globalConfirmationKey, alwaysAskUserConfirm.isSelected());
        }
        return changed;
    }
}
