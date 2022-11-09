// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.imagery.street_level.Projections;

/**
 * An interface for images with metadata
 * @author Taylor Smock
 * @since 18592, largely extracted from {@link org.openstreetmap.josm.data.gpx.GpxImageEntry}
 */
public interface ImageMetadata {
    /**
     * Get the image location
     * @return The image location
     */
    URI getImageURI();

    /**
     * Returns width of the image this ImageMetadata represents.
     * @return width of the image this ImageMetadata represents
     */
    int getWidth();

    /**
     * Returns height of the image this ImageMetadata represents.
     * @return height of the image this ImageMetadata represents
     * @since 18592 (interface), 13220 (GpxImageEntry)
     */
    int getHeight();

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
     * Returns the GPS time value. The GPS time value from the temporary copy
     * is returned if that copy exists.
     * @return the GPS time value
     */
    Instant getGpsInstant();

    /**
     * Convenient way to determine if this entry has a GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a GPS time
     * @since 18592 (interface), 6450 (GpxImageEntry)
     */
    boolean hasGpsTime();

    /**
     * Returns a display name for this entry
     * @return a display name for this entry
     */
    String getDisplayName();

    /**
     * Returns EXIF orientation
     * @return EXIF orientation
     */
    default Integer getExifOrientation() {
        return 1;
    }

    /**
     * Returns EXIF time
     * @return EXIF time
     * @since 18592 (interface), 17715 (GpxImageEntry)
     */
    Instant getExifInstant();

    /**
     * Convenient way to determine if this entry has a EXIF time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF time
     * @since 18592 (interface), 6450 (GpxImageEntry)
     */
    boolean hasExifTime();

    /**
     * Returns the EXIF GPS time.
     * @return the EXIF GPS time
     * @since 18592 (interface), 17715 (GpxImageEntry)
     */
    Instant getExifGpsInstant();

    /**
     * Convenient way to determine if this entry has a EXIF GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF GPS time
     * @since 18592 (interface), 6450 (GpxImageEntry)
     */
    boolean hasExifGpsTime();

    /**
     * Get the exif coordinates
     * @return The location of the image
     */
    ILatLon getExifCoor();

    /**
     * Get the exif direction
     * @return The image direction
     */
    Double getExifImgDir();

    /**
     * Get the last time the source was modified
     * @return The last time the source was modified
     */
    Instant getLastModified();

    /**
     * Sets the width of this ImageMetadata.
     * @param width set the width of this ImageMetadata
     * @since 18592 (interface), 13220 (GpxImageEntry)
     */
    void setWidth(int width);

    /**
     * Sets the height of this ImageMetadata.
     * @param height set the height of this ImageMetadata
     * @since 18592 (interface), 13220 (GpxImageEntry)
     */
    void setHeight(int height);

    /**
     * Sets the position.
     * @param pos position (will be cached)
     */
    void setPos(ILatLon pos);

    /**
     * Sets the speed.
     * @param speed speed
     */
    void setSpeed(Double speed);

    /**
     * Sets the elevation.
     * @param elevation elevation
     */
    void setElevation(Double elevation);

    /**
     * Sets EXIF orientation.
     * @param exifOrientation EXIF orientation
     */
    void setExifOrientation(Integer exifOrientation);

    /**
     * Sets EXIF time.
     * @param exifTime EXIF time
     * @since 18592 (interface), 17715 (GpxImageEntry)
     */
    void setExifTime(Instant exifTime);

    /**
     * Sets the EXIF GPS time.
     * @param exifGpsTime the EXIF GPS time
     * @since 18592 (interface), 17715 (GpxImageEntry)
     */
    void setExifGpsTime(Instant exifGpsTime);

    /**
     * Sets the GPS time.
     * @param gpsTime the GPS time
     * @since 18592 (interface), 17715 (GpxImageEntry)
     */
    void setGpsTime(Instant gpsTime);

    /**
     * Set the exif coordinates
     * @param exifCoor The exif coordinates
     */
    void setExifCoor(ILatLon exifCoor);

    /**
     * Set the exif direction
     * @param exifDir The direction
     */
    void setExifImgDir(Double exifDir);

    /**
     * Sets the IPTC caption.
     * @param iptcCaption the IPTC caption
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    void setIptcCaption(String iptcCaption);

    /**
     * Sets the IPTC headline.
     * @param iptcHeadline the IPTC headline
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    void setIptcHeadline(String iptcHeadline);

    /**
     * Sets the IPTC keywords.
     * @param iptcKeywords the IPTC keywords
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    void setIptcKeywords(List<String> iptcKeywords);

    /**
     * Sets the IPTC object name.
     * @param iptcObjectName the IPTC object name
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    void setIptcObjectName(String iptcObjectName);

    /**
     * Returns the IPTC caption.
     * @return the IPTC caption
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    String getIptcCaption();

    /**
     * Returns the IPTC headline.
     * @return the IPTC headline
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    String getIptcHeadline();

    /**
     * Returns the IPTC keywords.
     * @return the IPTC keywords
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    List<String> getIptcKeywords();

    /**
     * Returns the IPTC object name.
     * @return the IPTC object name
     * @since 18592 (interface), 15219 (GpxImageEntry)
     */
    String getIptcObjectName();

    /**
     * Extract GPS metadata from image EXIF. Has no effect if the image file is not set
     *
     * If successful, fills in the LatLon, speed, elevation, image direction, and other attributes
     * @since 18592 (interface), 9270 (GpxImageEntry)
     */
    default void extractExif() {
        try (InputStream original = getInputStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(original)) {
            ImageUtils.applyExif(this, bufferedInputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get the image input stream
     * @return The input stream of the image
     * @throws IOException If something happens during image read. See implementation for details.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Get the projection type for this entry
     * @return The projection type
     * @since 18592 (interface), 18246 (GpxImageEntry)
     */
    Projections getProjectionType();

    /**
     * Set the new projection type
     * @param newProjection The new type
     */
    void setProjectionType(Projections newProjection);
}
