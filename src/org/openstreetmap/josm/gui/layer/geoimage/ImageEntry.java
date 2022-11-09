// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import javax.imageio.IIOParam;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Stores info about each image, with an optional thumbnail
 * @since 2662
 */
public class ImageEntry extends GpxImageEntry implements IImageEntry<ImageEntry> {

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

    @Override
    public ImageEntry getNextImage() {
        return this.dataSet.getNextImage();
    }

    @Override
    public ImageEntry getPreviousImage() {
        return this.dataSet.getPreviousImage();
    }

    @Override
    public ImageEntry getFirstImage() {
        return this.dataSet.getFirstImage();
    }

    @Override
    public void selectImage(ImageViewerDialog imageViewerDialog, IImageEntry<?> entry) {
        if (entry instanceof ImageEntry) {
            this.dataSet.setSelectedImage((ImageEntry) entry);
        }
        imageViewerDialog.displayImages(this.dataSet.getLayer(), Collections.singletonList(entry));
    }

    @Override
    public ImageEntry getLastImage() {
        return this.dataSet.getLastImage();
    }

    @Override
    public boolean isRemoveSupported() {
        return true;
    }

    @Override
    public boolean remove() {
        this.dataSet.removeImage(this, true);
        return true;
    }

    @Override
    public boolean isDeleteSupported() {
        return true;
    }

    @Override
    public boolean delete() {
        return Utils.deleteFile(this.getFile());
    }

    /**
     * Reads the image represented by this entry in the given target dimension.
     * @param target the desired dimension used for {@linkplain IIOParam#setSourceSubsampling subsampling} or {@code null}
     * @return the read image, or {@code null}
     * @throws IOException if any I/O error occurs
     */
    @Override
    public BufferedImage read(Dimension target) throws IOException {
        URL imageUrl = getImageUrl();
        Logging.info(tr("Loading {0}", imageUrl));
        BufferedImage image = ImageProvider.read(imageUrl, false, false,
                r -> target == null ? r.getDefaultReadParam() : withSubsampling(r, target));
        if (image == null) {
            Logging.warn("Unable to load {0}", imageUrl);
            return null;
        }
        Logging.debug("Loaded {0} with dimensions {1}x{2} memoryTaken={3}m exifOrientationSwitchedDimension={4}",
                imageUrl, image.getWidth(), image.getHeight(), image.getWidth() * image.getHeight() * 4 / 1024 / 1024,
                ExifReader.orientationSwitchesDimensions(getExifOrientation()));
        return applyExifRotation(image);
    }

    protected URL getImageUrl() throws MalformedURLException {
        return getFile().toURI().toURL();
    }

    private static ImageReadParam withSubsampling(ImageReader reader, Dimension target) {
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

    private BufferedImage applyExifRotation(BufferedImage img) {
        Integer exifOrientation = getExifOrientation();
        if (!ExifReader.orientationNeedsCorrection(exifOrientation)) {
            return img;
        }
        boolean switchesDimensions = ExifReader.orientationSwitchesDimensions(exifOrientation);
        int width = switchesDimensions ? img.getHeight() : img.getWidth();
        int height = switchesDimensions ? img.getWidth() : img.getHeight();
        BufferedImage rotated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        AffineTransform transform = ExifReader.getRestoreOrientationTransform(exifOrientation, img.getWidth(), img.getHeight());
        Graphics2D g = rotated.createGraphics();
        g.drawImage(img, transform, null);
        g.dispose();
        return rotated;
    }
}
