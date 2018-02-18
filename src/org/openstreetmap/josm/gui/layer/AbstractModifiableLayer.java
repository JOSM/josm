// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.data.osm.ReadOnly;
import org.openstreetmap.josm.gui.io.AbstractIOTask;
import org.openstreetmap.josm.gui.io.AbstractUploadDialog;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * A modifiable layer.
 * @since 7358
 */
public abstract class AbstractModifiableLayer extends Layer implements UploadToServer, SaveToFile, ReadOnly {

    /**
     * Constructs a new {@code ModifiableLayer}.
     * @param name Layer name
     */
    public AbstractModifiableLayer(String name) {
        super(name);
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
    public void setReadOnly() {
        // Override if needed
    }

    @Override
    public void unsetReadOnly() {
        // Override if needed
    }

    @Override
    public boolean isReadOnly() {
        // Override if needed
        return false;
    }
}
