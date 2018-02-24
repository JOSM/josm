// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

/**
 * Interface for layers that can download data.
 * @see UploadToServer
 * @since 13453
 */
public interface DownloadFromServer {

    /**
     * Determines if the layer is able to download data and implements the
     * {@code DownloadFromServer} interface. A layer that implements the
     * {@code DownloadFromServer} interface must return {@code true}.
     *
     * @return {@code true} if the layer is able to download data; {@code false}, otherwise
     */
    boolean isDownloadable();
}
