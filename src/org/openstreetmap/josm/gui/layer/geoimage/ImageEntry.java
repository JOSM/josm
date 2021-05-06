// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Objects;

import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

import javax.imageio.IIOParam;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

import static org.openstreetmap.josm.tools.I18n.tr;

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

    /**
     * Reads the image represented by this entry in the given target dimension.
     * @param target the desired dimension used for {@linkplain IIOParam#setSourceSubsampling subsampling} or {@code null}
     * @return the read image
     * @throws IOException if any I/O error occurs
     */
    public BufferedImage read(Dimension target) throws IOException {
        Logging.info(tr("Loading {0}", getFile().getPath()));
        return ImageProvider.read(getFile(), false, false,
                r -> target == null ? r.getDefaultReadParam() : withSubsampling(r, target));
    }

    private ImageReadParam withSubsampling(ImageReader reader, Dimension target) {
        try {
            ImageReadParam param = reader.getDefaultReadParam();
            Dimension source = new Dimension(reader.getWidth(0), reader.getHeight(0));
            if (source.getWidth() > target.getWidth() || source.getHeight() > target.getHeight()) {
                int subsampling = (int) Math.floor(Math.max(
                        source.getWidth() / target.getWidth(),
                        source.getHeight() / target.getHeight()));
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
            }
            return param;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
