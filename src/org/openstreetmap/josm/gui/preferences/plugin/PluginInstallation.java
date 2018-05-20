// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

/**
 * Plugin installation status, used to filter plugin preferences model.
 * @since 13799
 */
public enum PluginInstallation {
    /** Plugins installed and loaded **/
    INSTALLED,
    /** Plugins not loaded **/
    AVAILABLE,
    /** All plugins **/
    ALL
}
