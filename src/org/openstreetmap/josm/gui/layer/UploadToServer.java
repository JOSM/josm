// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.AbstractUploadDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Interface for layers that can upload data.
 * @since 9751
 */
public interface UploadToServer {

    /**
     * Determines if the layer is able to upload data and implements the
     * {@code UploadToServer} interface.  A layer that implements the
     * {@code UploadToServer} interface must return {@code true}.
     *
     * @return {@code true} if the layer is able to upload data; {@code false}, otherwise
     */
    boolean isUploadable();

    /**
     * Determines if the data managed by this layer needs to be uploaded to
     * the server because it contains modified data.
     *
     * @return {@code true} if the data managed by this layer needs to be
     *         uploaded to the server because it contains modified data;
     *         {@code false}, otherwise
     */
    boolean requiresUploadToServer();

    /**
     * Determines if upload of data managed by this layer is discouraged.
     * This feature allows to use "private" data layers.
     *
     * @return {@code true} if upload is discouraged for this layer; {@code false}, otherwise
     */
    boolean isUploadDiscouraged();

    /**
     * Determines if upload of data managed by this layer is currently in progress.
     *
     * @return {@code true} if upload is in progress
     * @since 13434
     */
    boolean isUploadInProgress();

    /**
     * Initializes the layer after a successful upload to the server.
     */
    void onPostUploadToServer();

    /**
     * Creates a new {@code AbstractIOTask} for uploading data.
     * @param monitor The progress monitor
     * @return a new {@code AbstractIOTask} for uploading data, or {@code null} if not applicable
     */
    AbstractIOTask createUploadTask(ProgressMonitor monitor);

    /**
     * Returns the upload dialog for this layer.
     * @return the upload dialog for this layer, or {@code null} if not applicable
     */
    AbstractUploadDialog getUploadDialog();
}
