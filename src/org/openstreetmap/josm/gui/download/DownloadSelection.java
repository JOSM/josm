// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

public interface DownloadSelection  {

    /**
     * Add the GUI elements to the dialog.
     */
    void addGui(DownloadDialog gui);

    /**
     * Sets the current download area. The area may be null to clear
     * the current download area.
     *
     * @param are the current download area
     */
    public void setDownloadArea(Bounds area);
}
