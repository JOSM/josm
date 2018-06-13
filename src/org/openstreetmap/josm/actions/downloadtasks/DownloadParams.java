// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.UploadPolicy;

/**
 * Download parameters affecting the behaviour of {@link DownloadTask}s.
 * @since 13927
 */
public class DownloadParams {

    private boolean newLayer;
    private String layerName;
    private boolean locked;
    private DownloadPolicy downloadPolicy;
    private UploadPolicy uploadPolicy;

    /**
     * Determines if the data is to be downloaded into a new layer.
     * @return true, if the data is to be downloaded into a new layer. If false, the task
     * selects one of the existing layers as download layer, preferably the active layer.
     * @see #getLayerName
     */
    public final boolean isNewLayer() {
        return newLayer;
    }

    /**
     * Sets whether the data is to be downloaded into a new layer.
     * @param newLayer true, if the data is to be downloaded into a new layer. If false, the task
     * selects one of the existing layers as download layer, preferably the active layer.
     * @return this
     * @see #withLayerName
     */
    public final DownloadParams withNewLayer(boolean newLayer) {
        this.newLayer = newLayer;
        return this;
    }

    /**
     * Returns the new layer name (if a new layer is to be created).
     * @return the new layer name, or null
     * @see #isNewLayer
     */
    public final String getLayerName() {
        return layerName;
    }

    /**
     * Sets the new layer name (if a new layer is to be created).
     * @param layerName the new layer name, or null
     * @return this
     * @see #withNewLayer
     */
    public final DownloadParams withLayerName(String layerName) {
        this.layerName = layerName;
        return this;
    }

    /**
     * Determines if the new layer must be locked.
     * @return {@code true} if the new layer must be locked
     */
    public final boolean isLocked() {
        return locked;
    }

    /**
     * Sets whether the new layer must be locked.
     * @param locked {@code true} if the new layer must be locked
     * @return this
     */
    public final DownloadParams withLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    /**
     * Returns the download policy of new layer.
     * @return the download policy of new layer
     */
    public final DownloadPolicy getDownloadPolicy() {
        return downloadPolicy;
    }

    /**
     * Sets the download policy of new layer.
     * @param downloadPolicy the download policy of new layer
     * @return this
     */
    public final DownloadParams withDownloadPolicy(DownloadPolicy downloadPolicy) {
        this.downloadPolicy = downloadPolicy;
        return this;
    }

    /**
     * Returns the upload policy of new layer.
     * @return the upload policy of new layer
     */
    public final UploadPolicy getUploadPolicy() {
        return uploadPolicy;
    }

    /**
     * Sets the upload policy of new layer.
     * @param uploadPolicy the upload policy of new layer
     * @return this
     */
    public final DownloadParams withUploadPolicy(UploadPolicy uploadPolicy) {
        this.uploadPolicy = uploadPolicy;
        return this;
    }
}
