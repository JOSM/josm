// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

/**
 * A mean to select a download area in the download dialog.
 * Currently each selector implementation is accessible through its dedicated tab.
 * @since 2344
 */
public interface DownloadSelection {

    /**
     * Add the GUI elements to the dialog.
     * @param gui download dialog
     */
    void addGui(DownloadDialog gui);

    /**
     * Sets the current download area. The area may be null to clear
     * the current download area.
     *
     * @param area the current download area
     */
    void setDownloadArea(Bounds area);
}
