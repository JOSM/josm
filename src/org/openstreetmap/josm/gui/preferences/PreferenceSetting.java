// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

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
}
