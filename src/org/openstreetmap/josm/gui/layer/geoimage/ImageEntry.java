// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.util.Collections;

import org.openstreetmap.josm.data.gpx.GpxImageEntry;

/**
 * Stores info about each image, with an optional thumbnail
 * @since 2662
 */
public final class ImageEntry extends GpxImageEntry {

    private Image thumbnail;

    /**
     * Constructs a new {@code ImageEntry}.
     */
    public ImageEntry() {
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
}
