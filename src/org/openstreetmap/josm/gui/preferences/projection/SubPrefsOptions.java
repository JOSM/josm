// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

/**
 * ProjectionChoice can implement this interface to set some additional options.
 * @since 5226
 */
public interface SubPrefsOptions {

    /**
     * Determines if the projection code should be displayed in the top panel.
     * @return true, if the projection code should be displayed in the top panel
     */
    boolean showProjectionCode();

    /**
     * Determines if the projection name should be displayed in the top panel.
     * @return true, if the projection name should be displayed in the top panel
     */
    boolean showProjectionName();
}
