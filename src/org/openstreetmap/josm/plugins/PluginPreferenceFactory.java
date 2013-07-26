// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;

public class PluginPreferenceFactory implements PreferenceSettingFactory {

    private final PluginProxy plugin;

    public PluginPreferenceFactory(PluginProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public PreferenceSetting createPreferenceSetting() {
        return plugin.getPreferenceSetting();
    }

}
