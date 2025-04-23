// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.street_level;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import javax.imageio.IIOParam;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageMetadata;
import org.openstreetmap.josm.gui.layer.geoimage.ImageUtils;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * An interface for image entries that will be shown in {@link org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay}
 * @author Taylor Smock
 * @param <I> type of image
 * @since 18246
 */
public interface IImageEntry<I extends IImageEntry<I>> {
    /**
     * Select the next image
     * @param imageViewerDialog The image viewer to update
     */
    default void selectNextImage(final ImageViewerDialog imageViewerDialog) {
        this.selectImage(imageViewerDialog, this.getNextImage());
    }

    /**
     * Get what would be the next image
     * @return The next image
     */
    I getNextImage();

    /**
     * Select the previous image
     * @param imageViewerDialog The image viewer to update
     */
    default void selectPreviousImage(final ImageViewerDialog imageViewerDialog) {
        this.selectImage(imageViewerDialog, this.getPreviousImage());
    }

    /**
     * Get the previous image
     * @return The previous image
     */
    I getPreviousImage();

    /**
     * Select the first image for the data or sequence
     * @param imageViewerDialog The image viewer to update
     */
    default void selectFirstImage(final ImageViewerDialog imageViewerDialog) {
        this.selectImage(imageViewerDialog, this.getFirstImage());
    }

    /**
     * Get the first image for the data or sequence
     * @return The first image
     */
    I getFirstImage();

    /**
     * Select the last image for the data or sequence
     * @param imageViewerDialog The image viewer to update
     */
    default void selectLastImage(final ImageViewerDialog imageViewerDialog) {
        this.selectImage(imageViewerDialog, this.getLastImage());
    }

    /**
     * Select a specific image
     * @param imageViewerDialog The image viewer to update
     * @param entry The image to select
     * @since 18290
     */
    default void selectImage(final ImageViewerDialog imageViewerDialog, final IImageEntry<?> entry) {
        imageViewerDialog.displayImage(entry);
    }

    /**
     * Get the last image for the data or sequence
     * @return The last image
     */
    I getLastImage();

    /**
     * Remove the image
     * @return {@code true} if removal was successful
     * @throws UnsupportedOperationException If the implementation does not support removal.
     * Use {@link #isRemoveSupported()}} to check for support.
     */
    default boolean remove() {
        throw new UnsupportedOperationException("remove is not supported for " + this.getClass().getSimpleName());
    }

    /**
     * Check if image removal is supported
     * @return {@code true} if removal is supported
     */
    default boolean isRemoveSupported() {
        return false;
    }

    /**
     * Delete the image
     * @return {@code true} if deletion was successful
     * @throws UnsupportedOperationException If the implementation does not support deletion.
     * Use {@link #isDeleteSupported()}} to check for support.
     * @since 18278
     */
    default boolean delete() {
        throw new UnsupportedOperationException("remove is not supported for " + this.getClass().getSimpleName());
    }

    /**
     * Check if image deletion is supported
     * @return {@code true} if deletion is supported
     * @since 18278
     */
    default boolean isDeleteSupported() {
        return false;
    }

    /**
     * Returns a display name for this entry (shown in image viewer title bar)
     * @return a display name for this entry
     */
    String getDisplayName();

    /**
     * Reads the image represented by this entry in the given target dimension.
     * @param target the desired dimension used for {@linkplain IIOParam#setSourceSubsampling subsampling} or {@code null}
     * @return the read image, or {@code null}
     * @throws IOException if any I/O error occurs
     */
    default BufferedImage read(Dimension target) throws IOException {
        URI imageUrl = getImageURI();
        if (imageUrl == null) {
            return null;
        }
        Logging.info(tr("Loading {0}", imageUrl));
        BufferedImage image = ImageProvider.read(imageUrl.toURL(), false, false,
                r -> target == null ? r.getDefaultReadParam() : ImageUtils.withSubsampling(r, target));
        if (image == null) {
            Logging.warn("Unable to load {0}", imageUrl);
            return null;
        }
        if (this instanceof ImageMetadata) {
            ImageMetadata data = (ImageMetadata) this;
            Logging.debug("Loaded {0} with dimensions {1}x{2} memoryTaken={3}m exifOrientationSwitchedDimension={4}",
                    imageUrl, image.getWidth(), image.getHeight(), image.getWidth() * image.getHeight() * 4 / 1024 / 1024,
                    ExifReader.orientationSwitchesDimensions(data.getExifOrientation()));
            return ImageUtils.applyExifRotation(image, data.getExifOrientation());
        }
        return image;
    }

    /**
     * Sets the width of this ImageEntry.
     * @param width set the width of this ImageEntry
     */
    void setWidth(int width);

    /**
     * Sets the height of this ImageEntry.
     * @param height set the height of this ImageEntry
     */
    void setHeight(int height);

    /**
     * Returns associated file.
     * @return associated file
     */
    File getFile();

    /**
     * Get the URI for the image
     * @return The image URI
     * @since 18427
     */
    default URI getImageURI() {
        File file = this.getFile();
        if (file != null) {
            return file.toURI();
        }
        return null;
    }

    /**
     * Returns the position value. The position value from the temporary copy
     * is returned if that copy exists.
     * @return the position value
     */
    ILatLon getPos();

    /**
     * Returns the speed value. The speed value from the temporary copy is
     * returned if that copy exists.
     * @return the speed value
     */
    Double getSpeed();

    /**
     * Returns the elevation value. The elevation value from the temporary
     * copy is returned if that copy exists.
     * @return the elevation value
     */
    Double getElevation();

    /**
     * Return the GPS Differential mode value. The differential mode value from the temporary
     * copy is returned if that copy exists. 
     * @return the fix mode value
     * @since 19387
     */
    Integer getGpsDiffMode();

    /**
     * Return the GPS 2d/3d mode value. The 2d/3d mode value from the temporary
     * copy is returned if that copy exists.
     * @return the 2d/3d mode value
     * @since 19387
     */
    Integer getGps2d3dMode();

    /**
     * Return the GPS DOP value. The GPS DOP value from the temporary
     * copy is return if that copy exists.
     * @return the GPS DOP value
     * @since 19387
     */
    Double getExifGpsDop();

    /**
     * Return the GPS datum value. The GPS datum value from the temporary
     * copy is return if that copy exists. 
     * @return the GPS datum value
     * @since 19387
     */
    String getExifGpsDatum();

    /**
     * Return the GPS processing method. The processing method value from the temporary
     * copy is return if that copy exists.
     * @return the GPS processing method
     * @since 19387
     */
    String getExifGpsProcMethod();

    /**
     * Returns the image direction. The image direction from the temporary
     * copy is returned if that copy exists.
     * @return The image camera angle
     */
    Double getExifImgDir();
    
    /**
     * Returns the image GPS track direction. The GPS track direction from the temporary
     * copy is returned if that copy exists.
     * @return the image GPS track direction angle
     * @since 19387
     */
    Double getExifGpsTrack();

    /**
     * Returns the image horizontal positionning error. The image positionning error
     * from the temporary copy is returned if that copy exists.
     * @return the image horizontal positionning error
     * @since 19387
     */
    Double getExifHPosErr();
    
    /**
     * Convenient way to determine if this entry has a EXIF time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF time
     * @since 6450
     */
    boolean hasExifTime();

    /**
     * Returns EXIF time
     * @return EXIF time
     */
    Instant getExifInstant();

    /**
     * Convenient way to determine if this entry has a GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a GPS time
     */
    boolean hasGpsTime();

    /**
     * Returns the GPS time value. The GPS time value from the temporary copy
     * is returned if that copy exists.
     * @return the GPS time value
     */
    Instant getGpsInstant();

    /**
     * Returns the Exif GPS Time value. The Exif GPS time value from the temporary copy
     * is returned if that copy exists.
     * @return the Exif GPS time value
     * @since 19387
     */
    Instant getExifGpsInstant();

    /**
     * Returns the IPTC caption.
     * @return the IPTC caption
     */
    String getIptcCaption();

    /**
     * Returns the IPTC headline.
     * @return the IPTC headline
     */
    String getIptcHeadline();

    /**
     * Returns the IPTC keywords.
     * @return the IPTC keywords
     */
    List<String> getIptcKeywords();

    /**
     * Returns the IPTC object name.
     * @return the IPTC object name
     */
    String getIptcObjectName();

    /**
     * Get the camera projection type
     * @return the camera projection type
     */
    default Projections getProjectionType() {
        return Projections.PERSPECTIVE;
    }
}
