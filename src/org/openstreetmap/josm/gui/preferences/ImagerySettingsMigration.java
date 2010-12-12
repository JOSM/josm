// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.tools.GBC;

/** Migration of WMSPlugin and SlippyMap settings */
class ImagerySettingsMigration {
    enum PropertyMerge {
        USE_OLD,
        USE_NEW,
        THROW_ON_CONFLICT
    }
    static boolean wmsLayersConflict;
    static boolean wmsSettingsConflict;
    static boolean tmsSettingsConflict;

    private PropertyMerge mergeModel = PropertyMerge.THROW_ON_CONFLICT;

    static class SettingsConflictException extends Exception {
    }

    private void migrateProperty(String oldProp, String newProp)
    throws SettingsConflictException {
        String oldValue = Main.pref.get(oldProp, null);
        if (oldValue == null) return;
        if (mergeModel == PropertyMerge.THROW_ON_CONFLICT) {
            String newValue = Main.pref.get(newProp, null);
            if (newValue != null && !oldValue.equals(newValue)) {
                System.out.println(tr("Imagery settings migration: conflict when moving property {0} -> {1}",
                        oldProp, newProp));
                throw new SettingsConflictException();
            }
        }
        if (mergeModel != PropertyMerge.USE_NEW) {
            Main.pref.put(newProp, oldValue);
        }
        Main.pref.put(oldProp, null);
    }

    private void migrateArray(String oldProp, String newProp)
    throws SettingsConflictException {
        Collection<Collection<String>> oldValue = Main.pref.getArray(oldProp, null);
        if (oldValue == null) return;
        if (mergeModel == PropertyMerge.THROW_ON_CONFLICT) {
            Collection<Collection<String>> newValue = Main.pref.getArray(newProp, null);
            if (newValue != null) {
                System.out.println(tr("Imagery settings migration: conflict when moving array {0} -> {1}",
                        oldProp, newProp));
                throw new SettingsConflictException();
            }
        }
        if (mergeModel != PropertyMerge.USE_NEW) {
            Main.pref.putArray(newProp, oldValue);
        }
        Main.pref.putArray(oldProp, null);
    }

    private void migrateWMSPlugin() {
        try {
            Main.pref.put("wmslayers.default", null);
            migrateProperty("imagery.remotecontrol", "remotecontrol.permission.imagery");
            migrateProperty("wmsplugin.remotecontrol", "remotecontrol.permission.imagery");
            migrateProperty("wmsplugin.alpha_channel", "imagery.wms.alpha_channel");
            migrateProperty("wmsplugin.browser", "imagery.wms.browser");
            migrateProperty("wmsplugin.user_agent", "imagery.wms.user_agent");
            migrateProperty("wmsplugin.timeout.connect", "imagery.wms.timeout.connect");
            migrateProperty("wmsplugin.timeout.read", "imagery.wms.timeout.read");
            migrateProperty("wmsplugin.simultaneousConnections", "imagery.wms.simultaneousConnections");
            migrateProperty("wmsplugin.overlap", "imagery.wms.overlap");
            migrateProperty("wmsplugin.overlapEast", "imagery.wms.overlapEast");
            migrateProperty("wmsplugin.overlapNorth", "imagery.wms.overlapNorth");
            Map<String, String> unknownProps = Main.pref.getAllPrefix("wmsplugin");
            if (!unknownProps.isEmpty()) {
                System.out.println(tr("There are {0} unknown WMSPlugin settings", unknownProps.size()));
            }
        } catch (SettingsConflictException e) {
            wmsSettingsConflict = true;
        }
    }

    private void migrateSlippyMapPlugin() {
        try {
            Main.pref.put("slippymap.tile_source", null);
            Main.pref.put("slippymap.last_zoom_lvl", null);
            migrateProperty("slippymap.draw_debug", "imagery.tms.draw_debug");
            migrateProperty("slippymap.autoload_tiles", "imagery.tms.autoload");
            migrateProperty("slippymap.autozoom", "imagery.tms.autozoom");
            migrateProperty("slippymap.min_zoom_lvl", "imagery.tms.min_zoom_lvl");
            migrateProperty("slippymap.max_zoom_lvl", "imagery.tms.max_zoom_lvl");
            if (Main.pref.get("slippymap.fade_background_100", null) == null) {
                try {
                    Main.pref.putInteger("slippymap.fade_background_100", (int)Math.round(
                            Double.valueOf(Main.pref.get("slippymap.fade_background", "0"))*100.0));
                } catch (NumberFormatException e) {
                }
            }
            Main.pref.put("slippymap.fade_background", null);
            migrateProperty("slippymap.fade_background_100", "imagery.fade_amount");
            Map<String, String> unknownProps = Main.pref.getAllPrefix("slippymap");
            if (!unknownProps.isEmpty()) {
                System.out.println(tr("There are {0} unknown slippymap plugin settings", unknownProps.size()));
            }
        } catch (SettingsConflictException e) {
            tmsSettingsConflict = true;
        }
    }

    private void mergeWMSLayers() {
        ImageryLayerInfo layerInfo = ImageryLayerInfo.instance;
        HashSet<String> existingUrls = new HashSet<String>();
        for (ImageryInfo info : layerInfo.getLayers()) {
            existingUrls.add(info.getFullURL());
        }
        for(Collection<String> c : Main.pref.getArray("wmslayers",
                Collections.<Collection<String>>emptySet())) {
            ImageryInfo info = new ImageryInfo(c);
            if (!existingUrls.contains(info.getFullURL())) {
                layerInfo.add(info);
            }
        }
        layerInfo.save();
        Main.pref.putArray("wmslayers", null);
    }

    public void migrateSettings() {
        mergeModel = PropertyMerge.THROW_ON_CONFLICT;
        try {
            migrateArray("wmslayers", "imagery.layers");
        } catch (SettingsConflictException e) {
            wmsLayersConflict = true;
        }
        migrateWMSPlugin();
        migrateSlippyMapPlugin();
    }

    public boolean hasConflicts() {
        return wmsLayersConflict || wmsSettingsConflict || tmsSettingsConflict;
    }

    JRadioButton wlKeepImagery = new JRadioButton(tr("Keep current list"));
    JRadioButton wlUseWMS = new JRadioButton(tr("Overwrite with WMSPlugin list"));
    JRadioButton wlMerge = new JRadioButton(tr("Merge"));

    JRadioButton wsKeepImagery = new JRadioButton(tr("Keep current settings"));
    JRadioButton wsUseWMS = new JRadioButton(tr("Overwrite with WMSPlugin settings"));

    JRadioButton tsKeepImagery = new JRadioButton(tr("Keep current settings"));
    JRadioButton tsUseSlippyMap = new JRadioButton(tr("Overwrite with SlippyMap settings"));

    public void settingsMigrationDialog(Component parent) {
        JPanel p = new JPanel(new GridBagLayout());

        if (wmsLayersConflict) {
            p.add(new JLabel(tr("WMS layer list:")), GBC.eol());
            ButtonGroup g = new ButtonGroup();
            g.add(wlKeepImagery);
            g.add(wlUseWMS);
            g.add(wlMerge);
            wlMerge.setSelected(true);

            p.add(wlKeepImagery, GBC.eol());
            p.add(wlUseWMS, GBC.eol());
            p.add(wlMerge, GBC.eop());
        }

        if (wmsSettingsConflict) {
            p.add(new JLabel(tr("WMSPlugin settings:")), GBC.eol());
            ButtonGroup g = new ButtonGroup();
            g.add(wsKeepImagery);
            g.add(wsUseWMS);
            wsKeepImagery.setSelected(true);

            p.add(wsKeepImagery, GBC.eol());
            p.add(wsUseWMS, GBC.eop());
        }

        if (tmsSettingsConflict) {
            p.add(new JLabel(tr("SlippyMap settings:")), GBC.eol());
            ButtonGroup g = new ButtonGroup();
            g.add(tsKeepImagery);
            g.add(tsUseSlippyMap);
            tsKeepImagery.setSelected(true);

            p.add(tsKeepImagery, GBC.eol());
            p.add(tsUseSlippyMap, GBC.eop());
        }
        int answer = JOptionPane.showConfirmDialog(
                parent, p,
                tr("Imagery settings migration"),
                JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            if (wlMerge.isSelected()) {
                mergeWMSLayers();
            } else {
                mergeModel = wlKeepImagery.isSelected() ? PropertyMerge.USE_NEW : PropertyMerge.USE_OLD;
                migrateArray("wmslayers", "imagery.layers");
            }
            wmsLayersConflict = false;
            mergeModel = wsKeepImagery.isSelected() ? PropertyMerge.USE_NEW : PropertyMerge.USE_OLD;
            migrateWMSPlugin();
            wmsSettingsConflict = false;
            mergeModel = tsKeepImagery.isSelected() ? PropertyMerge.USE_NEW : PropertyMerge.USE_OLD;
            migrateSlippyMapPlugin();
            tmsSettingsConflict = false;
        } catch (SettingsConflictException e) {
            JOptionPane.showMessageDialog(
                    parent, tr("Warning: unexpected settings conflict"),
                    tr("Imagery settings migration"),
                    JOptionPane.OK_OPTION);
        }
    }
}
