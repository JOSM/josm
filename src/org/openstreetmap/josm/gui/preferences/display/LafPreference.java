// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;

/**
 * Look-and-feel preferences.
 */
public class LafPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code LafPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new LafPreference();
        }
    }

    /**
     * ComboBox with all look and feels.
     */
    private JosmComboBox lafCombo;
    JPanel panel;
    private JCheckBox showSplashScreen = new JCheckBox(tr("Show splash screen at startup"));
    private JCheckBox showID = new JCheckBox(tr("Show object ID in selection lists"));
    private JCheckBox showLocalizedName = new JCheckBox(tr("Show localized name in selection lists"));
    private JCheckBox modeless = new JCheckBox(tr("Modeless working (Potlatch style)"));
    private JCheckBox dynamicButtons = new JCheckBox(tr("Dynamic buttons in side menus"));

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        lafCombo = new JosmComboBox(UIManager.getInstalledLookAndFeels());

        // let's try to load additional LookAndFeels and put them into the list
        try {
            Class<?> Cquaqua = Class.forName("ch.randelshofer.quaqua.QuaquaLookAndFeel");
            Object Oquaqua = Cquaqua.getConstructor((Class[])null).newInstance((Object[])null);
            // no exception? Then Go!
            lafCombo.addItem(
                    new UIManager.LookAndFeelInfo(((javax.swing.LookAndFeel)Oquaqua).getName(), "ch.randelshofer.quaqua.QuaquaLookAndFeel")
            );
        } catch (Exception ex) {
            // just debug, Quaqua may not even be installed...
            Main.debug(ex.getMessage());
        }

        String laf = Main.pref.get("laf", Main.platform.getDefaultStyle());
        for (int i = 0; i < lafCombo.getItemCount(); ++i) {
            if (((LookAndFeelInfo)lafCombo.getItemAt(i)).getClassName().equals(laf)) {
                lafCombo.setSelectedIndex(i);
                break;
            }
        }

        final ListCellRenderer oldRenderer = lafCombo.getRenderer();
        lafCombo.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
            }
        });

        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // Show splash screen on startup
        showSplashScreen.setToolTipText(tr("Show splash screen at startup"));
        showSplashScreen.setSelected(Main.pref.getBoolean("draw.splashscreen", true));
        panel.add(showSplashScreen, GBC.eop().insets(20, 0, 0, 0));

        // Show ID in selection
        showID.setToolTipText(tr("Show object ID in selection lists"));
        showID.setSelected(Main.pref.getBoolean("osm-primitives.showid", false));

        // Show localized names
        showLocalizedName.setToolTipText(tr("Show localized name in selection lists, if available"));
        showLocalizedName.setSelected(Main.pref.getBoolean("osm-primitives.localize-name", true));
        ExpertToggleAction.addVisibilitySwitcher(showLocalizedName);

        modeless.setToolTipText(tr("Do not require to switch modes (potlatch style workflow)"));
        modeless.setSelected(Main.pref.getBoolean("modeless", false));
        ExpertToggleAction.addVisibilitySwitcher(modeless);

        panel.add(showID, GBC.eop().insets(20, 0, 0, 0));
        panel.add(showLocalizedName, GBC.eop().insets(20, 0, 0, 0));
        panel.add(modeless, GBC.eop().insets(20, 0, 0, 0));

        dynamicButtons.setToolTipText(tr("Display buttons in right side menus only when mouse is inside the element"));
        dynamicButtons.setSelected(ToggleDialog.PROP_DYNAMIC_BUTTONS.get());
        panel.add(dynamicButtons, GBC.eop().insets(20, 0, 0, 0));

        panel.add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        panel.add(new JLabel(tr("Look and Feel")), GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.getDisplayPreference().addSubTab(this, tr("Look and Feel"), scrollpane);
    }

    @Override
    public boolean ok() {
        boolean mod = false;
        Main.pref.put("draw.splashscreen", showSplashScreen.isSelected());
        Main.pref.put("osm-primitives.showid", showID.isSelected());
        Main.pref.put("osm-primitives.localize-name", showLocalizedName.isSelected());
        Main.pref.put("modeless", modeless.isSelected());
        Main.pref.put(ToggleDialog.PROP_DYNAMIC_BUTTONS.getKey(), dynamicButtons.isSelected());
        mod |= Main.pref.put("laf", ((LookAndFeelInfo)lafCombo.getSelectedItem()).getClassName());
        return mod;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }
}
