// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

public interface SubPreferenceSetting extends PreferenceSetting {
    
    /**
     * Returns the preference setting (displayed in the specified preferences tab pane) that contains this preference setting.
     */
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui);
}
