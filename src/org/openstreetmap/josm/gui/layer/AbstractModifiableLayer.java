// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.osm.Lockable;
import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.AbstractUploadDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * A modifiable layer.
 * @since 7358
 */
public abstract class AbstractModifiableLayer extends Layer implements DownloadFromServer, UploadToServer, SaveToFile, Lockable {

    /**
     * Constructs a new {@code ModifiableLayer}.
     * @param name Layer name
     */
    public AbstractModifiableLayer(String name) {
        super(name);
    }

    @Override
    public boolean isDownloadable() {
        // Override if needed
        return false;
    }

    @Override
    public boolean isUploadable() {
        // Override if needed
        return false;
    }

    @Override
    public boolean requiresUploadToServer() {
        // Override if needed
        return false;
    }

    @Override
    public boolean requiresSaveToFile() {
        // Override if needed
        return false;
    }

    @Override
    public boolean isUploadDiscouraged() {
        // Override if needed
        return false;
    }

    /**
     * Determines if data managed by this layer has been modified.
     * @return true if data has been modified; false, otherwise
     */
    public abstract boolean isModified();

    @Override
    public void onPostSaveToFile() {
        // Override if needed
    }

    /**
     * Initializes the layer after a successful upload to the server.
     */
    @Override
    public void onPostUploadToServer() {
        // Override if needed
    }

    @Override
    public AbstractIOTask createUploadTask(ProgressMonitor monitor) {
        // Override if needed
        return null;
    }

    @Override
    public AbstractUploadDialog getUploadDialog() {
        // Override if needed
        return null;
    }

    @Override
    public boolean isUploadInProgress() {
        // Override if needed
        return false;
    }

    @Override
    public void lock() {
        // Override if needed
    }

    @Override
    public void unlock() {
        // Override if needed
    }

    @Override
    public boolean isLocked() {
        // Override if needed
        return false;
    }

    /**
     * Perform the autosave action for the layer
     *
     * @param file The file to save to
     * @return {@code true} if the layer was successfully saved
     * @throws IOException If there was an IO exception from saving
     * @since 16548
     */
    public boolean autosave(File file) throws IOException {
        // Override if needed;
        return false;
    }

    /**
     * Get the data for the modifiable layer
     *
     * @return The data object
     * @since 16548
     */
    public Data getData() {
        // Override if needed;
        return null;
    }
}
