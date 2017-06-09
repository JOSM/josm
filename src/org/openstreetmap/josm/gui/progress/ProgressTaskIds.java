// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.progress;

/**
 * The default {@link ProgressTaskId}s used in JOSM
 */
public interface ProgressTaskIds {

    /**
     * Download GPS data
     */
    ProgressTaskId DOWNLOAD_GPS = new ProgressTaskId("core", "downloadGps");

    /**
     * Download WMS data along a gps line
     */
    ProgressTaskId PRECACHE_WMS = new ProgressTaskId("core", "precacheWms");

}
