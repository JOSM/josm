// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

/**
 * Preference settings, that do *not* display a top level tab.
 *
 * This preference setting's addGui method is called after the user clicked the parent tab
 * (returned by getTabPreferenceSetting).
 */
public interface SubPreferenceSetting extends PreferenceSetting {

    /**
     * Returns the preference setting (displayed in the specified preferences tab pane) that contains this preference setting.
     */
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui);
}
