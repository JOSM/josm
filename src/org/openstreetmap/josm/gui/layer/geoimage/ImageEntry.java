// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;

/**
 * Stores info about each image
 */
public final class ImageEntry implements Comparable<ImageEntry>, Cloneable {
    private File file;
    private Integer exifOrientation;
    private LatLon exifCoor;
    private Double exifImgDir;
    private Date exifTime;
    /**
     * Flag isNewGpsData indicates that the GPS data of the image is new or has changed.
     * GPS data includes the position, speed, elevation, time (e.g. as extracted from the GPS track).
     * The flag can used to decide for which image file the EXIF GPS data is (re-)written.
     */
    private boolean isNewGpsData;
    /** Temporary source of GPS time if not correlated with GPX track. */
    private Date exifGpsTime;
    private Image thumbnail;

    /**
     * The following values are computed from the correlation with the gpx track
     * or extracted from the image EXIF data.
     */
    private CachedLatLon pos;
    /** Speed in kilometer per hour */
    private Double speed;
    /** Elevation (altitude) in meters */
    private Double elevation;
    /** The time after correlation with a gpx track */
    private Date gpsTime;

    /**
     * When the correlation dialog is open, we like to show the image position
     * for the current time offset on the map in real time.
     * On the other hand, when the user aborts this operation, the old values
     * should be restored. We have a temporary copy, that overrides
     * the normal values if it is not null. (This may be not the most elegant
     * solution for this, but it works.)
     */
    ImageEntry tmp;

    /**
     * Constructs a new {@code ImageEntry}.
     */
    public ImageEntry() {}

    /**
     * Constructs a new {@code ImageEntry}.
     * @param file Path to image file on disk
     */
    public ImageEntry(File file) {
        setFile(file);
    }

    /**
     * Returns the position value. The position value from the temporary copy
     * is returned if that copy exists.
     * @return the position value
     */
    public CachedLatLon getPos() {
        if (tmp != null)
            return tmp.pos;
        return pos;
    }

    /**
     * Returns the speed value. The speed value from the temporary copy is
     * returned if that copy exists.
     * @return the speed value
     */
    public Double getSpeed() {
        if (tmp != null)
            return tmp.speed;
        return speed;
    }

    /**
     * Returns the elevation value. The elevation value from the temporary
     * copy is returned if that copy exists.
     * @return the elevation value
     */
    public Double getElevation() {
        if (tmp != null)
            return tmp.elevation;
        return elevation;
    }

    /**
     * Returns the GPS time value. The GPS time value from the temporary copy
     * is returned if that copy exists.
     * @return the GPS time value
     */
    public Date getGpsTime() {
        if (tmp != null)
            return getDefensiveDate(tmp.gpsTime);
        return getDefensiveDate(gpsTime);
    }

    /**
     * Convenient way to determine if this entry has a GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a GPS time
     * @since 6450
     */
    public boolean hasGpsTime() {
        return (tmp != null && tmp.gpsTime != null) || gpsTime != null;
    }

    /**
     * Returns associated file.
     * @return associated file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns EXIF orientation
     * @return EXIF orientation
     */
    public Integer getExifOrientation() {
        return exifOrientation;
    }

    /**
     * Returns EXIF time
     * @return EXIF time
     */
    public Date getExifTime() {
        return getDefensiveDate(exifTime);
    }

    /**
     * Convenient way to determine if this entry has a EXIF time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF time
     * @since 6450
     */
    public boolean hasExifTime() {
        return exifTime != null;
    }

    /**
     * Returns the EXIF GPS time.
     * @return the EXIF GPS time
     * @since 6392
     */
    public Date getExifGpsTime() {
        return getDefensiveDate(exifGpsTime);
    }

    /**
     * Convenient way to determine if this entry has a EXIF GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF GPS time
     * @since 6450
     */
    public boolean hasExifGpsTime() {
        return exifGpsTime != null;
    }

    private static Date getDefensiveDate(Date date) {
        if (date == null)
            return null;
        return new Date(date.getTime());
    }

    public LatLon getExifCoor() {
        return exifCoor;
    }

    public Double getExifImgDir() {
        if (tmp != null)
            return tmp.exifImgDir;
        return exifImgDir;
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

    /**
     * Sets the position.
     * @param pos cached position
     */
    public void setPos(CachedLatLon pos) {
        this.pos = pos;
    }

    /**
     * Sets the position.
     * @param pos position (will be cached)
     */
    public void setPos(LatLon pos) {
        setPos(pos != null ? new CachedLatLon(pos) : null);
    }

    /**
     * Sets the speed.
     * @param speed speed
     */
    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    /**
     * Sets the elevation.
     * @param elevation elevation
     */
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    /**
     * Sets associated file.
     * @param file associated file
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets EXIF orientation.
     * @param exifOrientation EXIF orientation
     */
    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }

    /**
     * Sets EXIF time.
     * @param exifTime EXIF time
     */
    public void setExifTime(Date exifTime) {
        this.exifTime = getDefensiveDate(exifTime);
    }

    /**
     * Sets the EXIF GPS time.
     * @param exifGpsTime the EXIF GPS time
     * @since 6392
     */
    public void setExifGpsTime(Date exifGpsTime) {
        this.exifGpsTime = getDefensiveDate(exifGpsTime);
    }

    public void setGpsTime(Date gpsTime) {
        this.gpsTime = getDefensiveDate(gpsTime);
    }

    public void setExifCoor(LatLon exifCoor) {
        this.exifCoor = exifCoor;
    }

    public void setExifImgDir(Double exifDir) {
        this.exifImgDir = exifDir;
    }

    @Override
    public ImageEntry clone() {
        try {
            return (ImageEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int compareTo(ImageEntry image) {
        if (exifTime != null && image.exifTime != null)
            return exifTime.compareTo(image.exifTime);
        else if (exifTime == null && image.exifTime == null)
            return 0;
        else if (exifTime == null)
            return -1;
        else
            return 1;
    }

    /**
     * Make a fresh copy and save it in the temporary variable. Use
     * {@link #applyTmp()} or {@link #discardTmp()} if the temporary variable
     * is not needed anymore.
     */
    public void createTmp() {
        tmp = clone();
        tmp.tmp = null;
    }

    /**
     * Get temporary variable that is used for real time parameter
     * adjustments. The temporary variable is created if it does not exist
     * yet. Use {@link #applyTmp()} or {@link #discardTmp()} if the temporary
     * variable is not needed anymore.
     * @return temporary variable
     */
    public ImageEntry getTmp() {
        if (tmp == null) {
            createTmp();
        }
        return tmp;
    }

    /**
     * Copy the values from the temporary variable to the main instance. The
     * temporary variable is deleted.
     * @see #discardTmp()
     */
    public void applyTmp() {
        if (tmp != null) {
            pos = tmp.pos;
            speed = tmp.speed;
            elevation = tmp.elevation;
            gpsTime = tmp.gpsTime;
            exifImgDir = tmp.exifImgDir;
            tmp = null;
        }
    }

    /**
     * Delete the temporary variable. Temporary modifications are lost.
     * @see #applyTmp()
     */
    public void discardTmp() {
        tmp = null;
    }

    /**
     * If it has been tagged i.e. matched to a gpx track or retrieved lat/lon from exif
     * @return {@code true} if it has been tagged
     */
    public boolean isTagged() {
        return pos != null;
    }

    /**
     * String representation. (only partial info)
     */
    @Override
    public String toString() {
        return file.getName()+": "+
        "pos = "+pos+" | "+
        "exifCoor = "+exifCoor+" | "+
        (tmp == null ? " tmp==null" :
            " [tmp] pos = "+tmp.pos);
    }

    /**
     * Indicates that the image has new GPS data.
     * That flag is set by new GPS data providers.  It is used e.g. by the photo_geotagging plugin
     * to decide for which image file the EXIF GPS data needs to be (re-)written.
     * @since 6392
     */
    public void flagNewGpsData() {
        isNewGpsData = true;
   }

    /**
     * Remove the flag that indicates new GPS data.
     * The flag is cleared by a new GPS data consumer.
     */
    public void unflagNewGpsData() {
        isNewGpsData = false;
    }

    /**
     * Queries whether the GPS data changed.
     * @return {@code true} if GPS data changed, {@code false} otherwise
     * @since 6392
     */
    public boolean hasNewGpsData() {
        return isNewGpsData;
    }

    /**
     * Extract GPS metadata from image EXIF. Has no effect if the image file is not set
     *
     * If successful, fills in the LatLon, speed, elevation, image direction, and other attributes
     * @since 9270
     */
    public void extractExif() {

        Metadata metadata;

        if (file == null) {
            return;
        }

        try {
            metadata = JpegMetadataReader.readMetadata(file);
        } catch (CompoundException | IOException ex) {
            Logging.error(ex);
            setExifTime(null);
            setExifCoor(null);
            setPos(null);
            return;
        }

        // Changed to silently cope with no time info in exif. One case
        // of person having time that couldn't be parsed, but valid GPS info
        try {
            setExifTime(ExifReader.readTime(metadata));
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException ex) {
            Logging.warn(ex);
            setExifTime(null);
        }

        final Directory dirExif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

        try {
            if (dirExif != null) {
                int orientation = dirExif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                setExifOrientation(orientation);
            }
        } catch (MetadataException ex) {
            Logging.debug(ex);
        }

        if (dirGps == null) {
            setExifCoor(null);
            setPos(null);
            return;
        }

        final Double speed = ExifReader.readSpeed(dirGps);
        if (speed != null) {
            setSpeed(speed);
        }

        final Double ele = ExifReader.readElevation(dirGps);
        if (ele != null) {
            setElevation(ele);
        }

        try {
            final LatLon latlon = ExifReader.readLatLon(dirGps);
            setExifCoor(latlon);
            setPos(getExifCoor());
        } catch (MetadataException | IndexOutOfBoundsException ex) { // (other exceptions, e.g. #5271)
            Logging.error("Error reading EXIF from file: " + ex);
            setExifCoor(null);
            setPos(null);
        }

        try {
            final Double direction = ExifReader.readDirection(dirGps);
            if (direction != null) {
                setExifImgDir(direction);
            }
        } catch (IndexOutOfBoundsException ex) { // (other exceptions, e.g. #5271)
            Logging.debug(ex);
        }

        final Date gpsDate = dirGps.getGpsDate();
        if (gpsDate != null) {
            setExifGpsTime(gpsDate);
        }
    }
}
