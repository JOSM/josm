// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
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
     * Get the EXIF coordinates
     * @return The location of the image
     */
    ILatLon getExifCoor();

    /**
     * Get the EXIF direction
     * @return The image direction
     */
    Double getExifImgDir();

    /**
     * Get the EXIF GPS track direction.
     * @return The GPS track direction
     * @since 19387
     */
    Double getExifGpsTrack();

    /**
     * Get the EXIF Horizontal positioning error.
     * @return The image horizontal positioning error
     * @since 19387
     */
    Double getExifHPosErr();

    /**
     * Get the GPS Differential mode.
     * @return The image gnss fix mode
     * @since 19387
     */
    Integer getGpsDiffMode();
    
    /**
     * Get the GPS 2d/3d mode.
     * @return The image gnss 2d/3d mode
     * @since 19387
     */
    Integer getGps2d3dMode();

    /**
     * Get the EXIF GPS DOP value.
     * @return The image GPS DOP value
     * @since 19387
     */
    Double getExifGpsDop();

    /**
     * Get the GPS datum value.
     * @return The image GPS datum value
     * @since 19387
     */
    String getExifGpsDatum();

    /**
     * Get the EXIF GPS processing method.
     * @return The image GPS processing method
     * @since 19387
     */
    String getExifGpsProcMethod();

    /**
     * Get the last time the source was modified.
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
     * Sets the EXIF coordinates
     * @param exifCoor The EXIF coordinates
     */
    void setExifCoor(ILatLon exifCoor);

    /**
     * Sets the EXIF direction
     * @param exifDir The direction
     */
    void setExifImgDir(Double exifDir);

    /**
     * Sets the EXIF GPS track direction.
     * @param exifGpsTrack The GPS track direction
     * @since 19387
     */
    void setExifGpsTrack(Double exifGpsTrack);

    /**
     * Sets the EXIF horizontal positioning error.
     * @param exifHposErr the EXIF horizontal positionning error
     * @since 19387
     */
    void setExifHPosErr(Double exifHPosErr);

    /**
     * Sets the EXIF GPS DOP value.
     * @param exifGpsDop the EXIF GPS DOP value
     * @since 19387
     */
    void setExifGpsDop(Double exifGpsDop);

    /**
     * Sets the GPS Differential mode.
     * @param gpsDiffMode GPS Differential mode
     * @since 19387
     */
    void setGpsDiffMode(Integer gpsDiffMode);

    /**
     * Sets the GPS 2d/3d mode.
     * @param gps2d3dMode GPS 2d/3d mode
     * @since 19387
     */
    void setGps2d3dMode(Integer gps2d3dMode);

    /**
     * Sets the GPS datum value.
     * @param exifGpsDatum GPS datum
     * @since 19387
     */
    void setExifGpsDatum(String exifGpsDatum);

    /**
     * Sets the GPS processing method.
     * @param exifGpsProcMethod GPS processing method
     * @since 19387
     */
    void setExifGpsProcMethod(String exifGpsProcMethod);

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
     * <p>
     * If successful, fills in the LatLon, speed, elevation, image direction, and other attributes
     * @since 18592 (interface), 9270 (GpxImageEntry)
     */
    default void extractExif() {
        try (InputStream original = getInputStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(original)) {
            ImageUtils.applyExif(this, bufferedInputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            throw new UncheckedIOException(new IOException(e));
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
