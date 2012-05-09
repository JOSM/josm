// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

/**
 * ProjectionSubPrefs can implement this interface to set some additional options
 */
public interface SubPrefsOptions {

    /**
     * @return true, if the projection code should be displayed in the top panel
     */
    boolean showProjectionCode();
}
