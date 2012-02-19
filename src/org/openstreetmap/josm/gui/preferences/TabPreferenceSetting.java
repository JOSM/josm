// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

/**
 * Preference settings, that display a top level tab.
 *
 * This preference setting's addGui method is called after the user clicked the tab.
 */
public interface TabPreferenceSetting extends PreferenceSetting {
    
    /**
     * Called during preferences dialog initialization to display the preferences tab with the returned icon.
     * @return The icon name in the preferences folder.
     */
    public String getIconName();

    /**
     * Called during preferences tab initialization to display its title.
     * @return The title of this preferences tab.
     */
    String getTitle();
    
    /**
     * Called during preferences dialog initialization to display the preferences tab with the returned tooltip.
     * @return The tooltip of this preferences tab.
     */
    public String getTooltip();

    /**
     * Called during preferences tab initialization to display a description in one sentence for this tab. 
     * Will be displayedin italic under the title.
     * @return The description of this preferences tab.
     */
    public String getDescription();
}
