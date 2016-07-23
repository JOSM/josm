// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

@FunctionalInterface
public interface PreferenceSettingFactory {

    PreferenceSetting createPreferenceSetting();
}
