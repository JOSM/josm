// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.tools.GBC;

public class MapPaintPreference implements PreferenceSetting {
    private StyleSources sources;
    private JCheckBox enableIconDefault;
    private JCheckBox enableDefault;
    private JComboBox styleCombo = new JComboBox();

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new MapPaintPreference();
        }
    }

    public void addGui(final PreferenceDialog gui) {
        enableDefault = new JCheckBox(tr("Enable built-in defaults"),
                Main.pref.getBoolean("mappaint.style.enable-defaults", true));
        enableIconDefault = new JCheckBox(tr("Enable built-in icon defaults"),
                Main.pref.getBoolean("mappaint.icon.enable-defaults", true));

        sources = new StyleSources("mappaint.style.sources", "mappaint.icon.sources",
        "http://josm.openstreetmap.de/styles", false, tr("Map Paint Styles"));

        Collection<String> styles = new TreeSet<String>(MapPaintStyles.getStyles().getStyleNames());
        String defstyle = Main.pref.get("mappaint.style", "standard");
        styles.add(defstyle);
        for(String style : styles)
            styleCombo.addItem(style);

        styleCombo.setEditable(true);
        for (int i = 0; i < styleCombo.getItemCount(); ++i) {
            if (((String)styleCombo.getItemAt(i)).equals(defstyle)) {
                styleCombo.setSelectedIndex(i);
                break;
            }
        }

        JPanel panel = new JPanel(new GridBagLayout());
        JScrollPane scrollpane = new JScrollPane(panel);
        panel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        panel.add(enableDefault, GBC.std().insets(5,5,5,0));
        panel.add(enableIconDefault, GBC.eol().insets(5,5,5,0));

        panel.add(new JLabel(tr("Used style")), GBC.std().insets(5,5,0,5));
        panel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(styleCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,0,5,0));

        panel.add(sources, GBC.eol().fill(GBC.BOTH));
        gui.mapcontent.addTab(tr("Map Paint Styles"), scrollpane);
    }

    public boolean ok() {
        Boolean restart = Main.pref.put("mappaint.style.enable-defaults", enableDefault.getSelectedObjects() != null);
        if(Main.pref.put("mappaint.icon.enable-defaults", enableIconDefault.getSelectedObjects() != null))
          restart = true;
        if(sources.finish())
          restart = true;
        Main.pref.put("mappaint.style", styleCombo.getEditor().getItem().toString());
        return restart;
    }

    /**
     * Initialize the styles
     */
    public static void initialize() {
        MapPaintStyles.readFromPreferences();
    }
}
