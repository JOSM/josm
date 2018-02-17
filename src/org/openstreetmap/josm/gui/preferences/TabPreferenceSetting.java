// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import java.awt.Component;

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
    String getIconName();

    /**
     * Called during preferences tab initialization to display its title.
     * @return The title of this preferences tab.
     */
    String getTitle();

    /**
     * Called during preferences dialog initialization to display the preferences tab with the returned tooltip.
     * @return The tooltip of this preferences tab.
     */
    String getTooltip();

    /**
     * Called during preferences tab initialization to display a description in one sentence for this tab.
     * Will be displayed in italic under the title.
     * @return The description of this preferences tab.
     */
    String getDescription();

    /**
     * Adds a new sub preference settings tab with the given title and component.
     * @param sub The new sub preference settings.
     * @param title The tab title.
     * @param component The tab component.
     * @since 5631
     */
    void addSubTab(SubPreferenceSetting sub, String title, Component component);

    /**
     * Adds a new sub preference settings tab with the given title, component and tooltip.
     * @param sub The new sub preference settings.
     * @param title The tab title.
     * @param component The tab component.
     * @param tip The tab tooltip.
     * @since 5631
     */
    void addSubTab(SubPreferenceSetting sub, String title, Component component, String tip);

    /**
     * Registers a sub preference settings to an existing tab component.
     * @param sub The new sub preference settings.
     * @param component The component for which a tab already exists.
     * @since 5631
     */
    void registerSubTab(SubPreferenceSetting sub, Component component);

    /**
     * Returns the tab component related to the specified sub preference settings
     * @param sub The requested sub preference settings.
     * @return The component related to the specified sub preference settings, or null.
     * @since 5631
     */
    Component getSubTab(SubPreferenceSetting sub);

    /**
     * Selects the specified sub preference settings, if applicable. Not all Tab preference settings need to implement this.
     * @param subPref The sub preference settings to be selected.
     * @return true if the specified preference settings have been selected, false otherwise.
     * @since 5631
     */
    boolean selectSubTab(SubPreferenceSetting subPref);

    /**
     * Returns the help context for this preferences settings tab.
     * @return the help context for this preferences settings tab
     * @since 13431
     */
    String getHelpContext();
}
