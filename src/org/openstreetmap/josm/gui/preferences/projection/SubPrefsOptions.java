// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

/**
 * ProjectionChoice can implement this interface to set some additional options
 */
public interface SubPrefsOptions {

    /**
     * @return true, if the projection code should be displayed in the top panel
     */
    boolean showProjectionCode();

    /**
     * @return true, if the projection name should be displayed in the top panel
     */
    boolean showProjectionName();
}
