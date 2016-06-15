// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

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
