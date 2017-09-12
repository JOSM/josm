// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import javax.swing.JMenu;

/**
 * GUI elements for a {@link StyleSetting} class.
 * @since 12831
 */
public interface StyleSettingGui {

    /**
     * Adds the menu entry for the corresponding style setting to the menu
     * @param menu The menu to add the setting to
     */
    void addMenuEntry(JMenu menu);

}
