// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.util.Collections;
import java.util.Objects;

import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;

/**
 * Stores info about each image, with an optional thumbnail
 * @since 2662
 */
public final class ImageEntry extends GpxImageEntry {

    private Image thumbnail;
    private ImageData dataSet;

    /**
     * Constructs a new {@code ImageEntry}.
     */
    public ImageEntry() {
    }

    /**
     * Constructs a new {@code ImageEntry} from an existing instance.
     * @param other existing instance
     * @since 14625
     */
    public ImageEntry(ImageEntry other) {
        super(other);
        thumbnail = other.thumbnail;
        dataSet = other.dataSet;
    }

    /**
     * Constructs a new {@code ImageEntry}.
     * @param file Path to image file on disk
     */
    public ImageEntry(File file) {
        super(file);
    }

    /**
     * Determines whether a thumbnail is set
     * @return {@code true} if a thumbnail is set
     */
    public boolean hasThumbnail() {
        return thumbnail != null;
    }

    /**
     * Returns the thumbnail.
     * @return the thumbnail
     */
    public Image getThumbnail() {
        return thumbnail;
    }

    /**
     * Sets the thumbnail.
     * @param thumbnail thumbnail
     */
    public void setThumbnail(Image thumbnail) {
        this.thumbnail = thumbnail;
    }

    /**
     * Loads the thumbnail if it was not loaded yet.
     * @see ThumbsLoader
     */
    public void loadThumbnail() {
        if (thumbnail == null) {
            new ThumbsLoader(Collections.singleton(this)).run();
        }
    }

    @Override
    protected void tmpUpdated() {
        super.tmpUpdated();
        if (this.dataSet != null) {
            this.dataSet.fireNodeMoved(this);
        }
    }

    /**
     * Set the dataset for this image
     * @param imageData The dataset
     * @since 17574
     */
    public void setDataSet(ImageData imageData) {
        this.dataSet = imageData;
    }

    /**
     * Get the dataset for this image
     * @return The dataset
     * @since 17574
     */
    public ImageData getDataSet() {
        return this.dataSet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), thumbnail, dataSet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ImageEntry other = (ImageEntry) obj;
        return Objects.equals(thumbnail, other.thumbnail) && Objects.equals(dataSet, other.dataSet);
    }
}
