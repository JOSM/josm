// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.AbstractUploadDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * A modifiable layer.
 * @since 7358
 */
public abstract class AbstractModifiableLayer extends Layer {

    /**
     * Constructs a new {@code ModifiableLayer}.
     * @param name Layer name
     */
    public AbstractModifiableLayer(String name) {
        super(name);
    }

    /**
     * Determines if the data managed by this layer needs to be uploaded to
     * the server because it contains modified data.
     *
     * @return true if the data managed by this layer needs to be uploaded to
     * the server because it contains modified data; false, otherwise
     */
    public boolean requiresUploadToServer() {
        // Override if needed
        return false;
    }

    /**
     * Determines if the data managed by this layer needs to be saved to
     * a file. Only replies true if a file is assigned to this layer and
     * if the data managed by this layer has been modified since the last
     * save operation to the file.
     *
     * @return true if the data managed by this layer needs to be saved to a file
     */
    public boolean requiresSaveToFile() {
        // Override if needed
        return false;
    }

    /**
     * Determines if upload of data managed by this layer is discouraged.
     * This feature allows to use "private" data layers.
     *
     * @return true if upload is discouraged for this layer; false, otherwise
     */
    public boolean isUploadDiscouraged() {
        // Override if needed
        return false;
    }

    /**
     * Determines if data managed by this layer has been modified.
     * @return true if data has been modified; false, otherwise
     */
    public abstract boolean isModified();

    /**
     * Initializes the layer after a successful save of data to a file.
     */
    public void onPostSaveToFile() {
        // Override if needed
    }

    /**
     * Initializes the layer after a successful upload to the server.
     */
    public void onPostUploadToServer() {
        // Override if needed
    }

    /**
     * Creates a new {@code AbstractIOTask} for uploading data.
     * @param monitor The progress monitor
     * @return a new {@code AbstractIOTask} for uploading data, or {@code null} if not applicable
     */
    public AbstractIOTask createUploadTask(ProgressMonitor monitor) {
        // Override if needed
        return null;
    }

    /**
     * Returns the upload dialog for this layer.
     * @return the upload dialog for this layer, or {@code null} if not applicable
     */
    public AbstractUploadDialog getUploadDialog() {
        // Override if needed
        return null;
    }
}
