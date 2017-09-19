// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

/**
 * Listener to get notified about changes in the list of download sources.
 * @since 12878
 */
interface DownloadSourceListener {

    /**
     * Called when a download source has been added.
     * @param source the new added download source.
     */
    void downloadSourceAdded(DownloadSource<?> source);
}
