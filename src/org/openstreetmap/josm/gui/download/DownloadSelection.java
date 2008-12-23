// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;


public interface DownloadSelection {
    /**
     * Add the GUI elements to the dialog.
     */
    void addGui(DownloadDialog gui);

    /**
     * Update or clear display when a selection is made through another
     * DownloadSelection object
     */
    void boundingBoxChanged(DownloadDialog gui);

}
