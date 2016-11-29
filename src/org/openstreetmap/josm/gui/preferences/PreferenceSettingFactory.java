// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

/**
 * Factory for {@link PreferenceSetting}.
 * @since 1742
 */
@FunctionalInterface
public interface PreferenceSettingFactory {

    /**
     * Creates preference settings.
     * @return created preference settings
     */
    PreferenceSetting createPreferenceSetting();
}
