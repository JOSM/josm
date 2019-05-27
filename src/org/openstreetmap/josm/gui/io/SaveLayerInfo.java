// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.io.File;

import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * SaveLayerInfo represents the information, user preferences and save/upload states of
 * a layer which might be uploaded/saved.
 * @since 2025
 */
class SaveLayerInfo implements Comparable<SaveLayerInfo> {

    /** the modifiable layer */
    private final AbstractModifiableLayer layer;
    private boolean doCheckSaveConditions;
    private boolean doSaveToFile;
    private boolean doUploadToServer;
    private File file;
    private UploadOrSaveState uploadState;
    private UploadOrSaveState saveState;

    /**
     * Constructs a new {@code SaveLayerInfo}.
     * @param layer the layer. Must not be null.
     * @throws IllegalArgumentException if layer is null
     */
    SaveLayerInfo(AbstractModifiableLayer layer) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        this.doCheckSaveConditions = true;
        this.doSaveToFile = layer.requiresSaveToFile();
        this.doUploadToServer = layer.requiresUploadToServer() && !layer.isUploadDiscouraged();
        this.file = layer.getAssociatedFile();
    }

    /**
     * Replies the layer this info objects holds information for
     *
     * @return the layer this info objects holds information for
     */
    public AbstractModifiableLayer getLayer() {
        return layer;
    }

    /**
     * Replies true if the layer can be saved to a file
     *
     * @return {@code true} if the layer can be saved to a file; {@code false} otherwise
     */
    public boolean isSavable() {
        return layer.isSavable();
    }

    /**
     * Replies true if the layer can be uploaded to a server
     *
     * @return {@code true} if the layer can be uploaded to a server; {@code false} otherwise
     */
    public boolean isUploadable() {
        return layer.isUploadable();
    }

    /**
     * Replies true if preconditions should be checked before saving; false, otherwise
     *
     * @return true if preconditions should be checked before saving; false, otherwise
     * @since 7204
     */
    public boolean isDoCheckSaveConditions() {
        return doCheckSaveConditions;
    }

    /**
     * Sets whether preconditions should be checked before saving
     *
     * @param doCheckSaveConditions true to check save preconditions; false, to skip checking
     * @since 7204
     */
    public void setDoCheckSaveConditions(boolean doCheckSaveConditions) {
        this.doCheckSaveConditions = doCheckSaveConditions;
    }

    /**
     * Replies true if this layer should be saved to a file; false, otherwise
     *
     * @return true if this layers should be saved to a file; false, otherwise
     */
    public boolean isDoSaveToFile() {
        return doSaveToFile;
    }

    /**
     * Sets whether this layer should be saved to a file
     *
     * @param doSaveToFile true to save; false, to skip saving
     */
    public void setDoSaveToFile(boolean doSaveToFile) {
        this.doSaveToFile = isSavable() && doSaveToFile;
    }

    /**
     * Replies true if this layer should be uploaded to the server; false, otherwise
     *
     * @return {@code true} if this layer should be uploaded to the server; {@code false}, otherwise
     */
    public boolean isDoUploadToServer() {
        return doUploadToServer;
    }

    /**
     * Sets whether this layer should be uploaded to a server
     *
     * @param doUploadToServer {@code true} to upload; {@code false}, to skip uploading
     */
    public void setDoUploadToServer(boolean doUploadToServer) {
        this.doUploadToServer = isUploadable() && doUploadToServer;
    }

    /**
     * Replies true if this layer should be uploaded to the server and saved to file.
     *
     * @return true if this layer should be uploaded to the server and saved to file
     */
    public boolean isDoSaveAndUpload() {
        return isDoSaveToFile() && isDoUploadToServer();
    }

    /**
     * Replies the name of the layer
     *
     * @return the name of the layer
     */
    public String getName() {
        return layer.getName() == null ? "" : layer.getName();
    }

    /**
     * Replies the file this layer should be saved to, if {@link #isDoSaveToFile()} is true
     *
     * @return the file this layer should be saved to, if {@link #isDoSaveToFile()} is true
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file this layer should be saved to, if {@link #isDoSaveToFile()} is true
     *
     * @param file the file
     */
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public int compareTo(SaveLayerInfo o) {
        if (isDoSaveAndUpload()) {
            if (o.isDoSaveAndUpload())
                return getName().compareTo(o.getName());
            return -1;
        } else if (o.isDoSaveAndUpload())
            return 1;
        if (isDoUploadToServer()) {
            if (o.isDoUploadToServer())
                return getName().compareTo(o.getName());
            return -1;
        } else if (o.isDoUploadToServer())
            return 1;
        if (isDoSaveToFile()) {
            if (o.isDoSaveToFile())
                return getName().compareTo(o.getName());
            return -1;
        } else if (o.isDoSaveToFile())
            return 1;
        return getName().compareTo(o.getName());
    }

    /**
     * Replies the upload state of {@link #getLayer()}.
     * <ul>
     *   <li>{@link UploadOrSaveState#OK} if {@link #getLayer()} was successfully uploaded</li>
     *   <li>{@link UploadOrSaveState#CANCELED} if uploading {@link #getLayer()} was canceled</li>
     *   <li>{@link UploadOrSaveState#FAILED} if uploading {@link #getLayer()} has failed</li>
     * </ul>
     *
     * @return the upload state
     */
    public UploadOrSaveState getUploadState() {
        return uploadState;
    }

    /**
     * Sets the upload state for {@link #getLayer()}
     *
     * @param uploadState the upload state
     */
    public void setUploadState(UploadOrSaveState uploadState) {
        this.uploadState = uploadState;
    }

    /**
     * Replies the save state of {@link #getLayer()}.
     * <ul>
     *   <li>{@link UploadOrSaveState#OK} if {@link #getLayer()} was successfully saved to file</li>
     *   <li>{@link UploadOrSaveState#CANCELED} if saving {@link #getLayer()} was canceled</li>
     *   <li>{@link UploadOrSaveState#FAILED} if saving {@link #getLayer()} has failed</li>
     * </ul>
     *
     * @return the save state
     */
    public UploadOrSaveState getSaveState() {
        return saveState;
    }

    /**
     * Sets the save state for {@link #getLayer()}
     *
     * @param saveState save the upload state
     */
    public void setSaveState(UploadOrSaveState saveState) {
        this.saveState = saveState;
    }

    /**
     * Resets the upload and save state
     *
     * @see #setUploadState(UploadOrSaveState)
     * @see #setSaveState(UploadOrSaveState)
     * @see #getUploadState()
     * @see #getSaveState()
     */
    public void resetUploadAndSaveState() {
        this.uploadState = null;
        this.saveState = null;
    }
}
