//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences.map;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

public class MapPreference extends DefaultTabPreferenceSetting {
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new MapPreference();
        }
    }
    
    private MapPreference() {
        super("map", tr("Map Settings"), tr("Settings for the map projection and data interpretation."), false, new JTabbedPane());
    }
    
    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(GBC.BOTH));
    }
}
