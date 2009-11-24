// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.io.File;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * SaveLayerInfo represents the information, user preferences and save/upload states of
 * a layer which might be uploaded/saved.
 *
 */
class SaveLayerInfo implements Comparable<SaveLayerInfo> {

    /** the osm data layer */
    private OsmDataLayer layer;
    private boolean doSaveToFile;
    private boolean doUploadToServer;
    private File file;
    private UploadOrSaveState uploadState;
    private UploadOrSaveState saveState;

    /**
     *
     * @param layer the layer. Must not be null.
     * @throws IllegalArgumentException thrown if layer is null
     */
    public SaveLayerInfo(OsmDataLayer layer) {
        if (layer == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "layer"));
        this.layer = layer;
        this.doSaveToFile = layer.requiresSaveToFile();
        this.doUploadToServer = layer.requiresUploadToServer();
        this.file = layer.getAssociatedFile();
    }

    /**
     * Replies the layer this info objects holds information for
     *
     * @return the layer this info objects holds information for
     */
    public OsmDataLayer getLayer() {
        return layer;
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
        this.doSaveToFile = doSaveToFile;
    }

    /**
     * Replies true if this layer should be uploaded to the server; false, otherwise
     *
     * @return true if this layer should be uploaded to the server; false, otherwise
     */
    public boolean isDoUploadToServer() {
        return doUploadToServer;
    }

    /**
     * Sets whether this layer should be uploaded to a file
     *
     * @param doSaveToFile true to upload; false, to skip uploading
     */

    public void setDoUploadToServer(boolean doUploadToServer) {
        this.doUploadToServer = doUploadToServer;
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
     * Replies the file this layer should be saved to, if {@see #isDoSaveToFile()} is true
     *
     * @return the file this layer should be saved to, if {@see #isDoSaveToFile()} is true
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file this layer should be saved to, if {@see #isDoSaveToFile()} is true
     *
     * @param file the file
     */
    public void setFile(File file) {
        this.file = file;
    }

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
     * Replies the upload state of {@see #getLayer()}.
     * <ul>
     *   <li>{@see UploadOrSaveState#OK} if {@see #getLayer() was successfully uploaded</li>
     *   <li>{@see UploadOrSaveState#CANCELLED} if uploading {@see #getLayer() was cancelled</li>
     *   <li>{@see UploadOrSaveState#FAILED} if uploading {@see #getLayer() has failed</li>
     * </ul>
     *
     * @return the upload state
     */
    public UploadOrSaveState getUploadState() {
        return uploadState;
    }

    /**
     * Sets the upload state for {@see #getLayer()}
     *
     * @param uploadState the upload state
     */
    public void setUploadState(UploadOrSaveState uploadState) {
        this.uploadState = uploadState;
    }

    /**
     * Replies the save state of {@see #getLayer()}.
     * <ul>
     *   <li>{@see UploadOrSaveState#OK} if {@see #getLayer() was successfully saved to file</li>
     *   <li>{@see UploadOrSaveState#CANCELLED} if saving {@see #getLayer() was cancelled</li>
     *   <li>{@see UploadOrSaveState#FAILED} if saving {@see #getLayer() has failed</li>
     * </ul>
     *
     * @return the save state
     */
    public UploadOrSaveState getSaveState() {
        return saveState;
    }

    /**
     * Sets the save state for {@see #getLayer()}
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
