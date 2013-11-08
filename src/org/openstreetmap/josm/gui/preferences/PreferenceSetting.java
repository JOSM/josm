// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

/**
 * Base interface of Preferences settings, should not be directly implemented,
 * see {@link TabPreferenceSetting} and {@link SubPreferenceSetting}.
 */
public interface PreferenceSetting {
    /**
     * Add the GUI elements to the dialog. The elements should be initialized after
     * the current preferences.
     */
    void addGui(PreferenceTabbedPane gui);

    /**
     * Called when OK is pressed to save the setting in the preferences file.
     * Return true when restart is required.
     */
    boolean ok();

    /**
     * Called to know if the preferences tab has only to be displayed in expert mode.
     * @return true if the tab has only to be displayed in expert mode, false otherwise.
     */
    public boolean isExpert();
}
