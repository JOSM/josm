// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;

/**
 * Preference settings factory for plugins.
 * @since 1742
 */
public class PluginPreferenceFactory implements PreferenceSettingFactory {

    private final PluginProxy plugin;

    /**
     * Constructs a new {@code PluginPreferenceFactory}.
     * @param plugin plugin proxy
     */
    public PluginPreferenceFactory(PluginProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public PreferenceSetting createPreferenceSetting() {
        return plugin.getPreferenceSetting();
    }

}
