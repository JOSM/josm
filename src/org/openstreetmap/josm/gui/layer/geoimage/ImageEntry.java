// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.util.Date;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;

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
    Image thumbnail;

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
     * should be restored. We have a temprary copy, that overrides
     * the normal values if it is not null. (This may be not the most elegant
     * solution for this, but it works.)
     */
    ImageEntry tmp;

    /**
     * Returns the cached temporary position value.
     * @return the cached temporary position value
     */
    public CachedLatLon getPos() {
        if (tmp != null)
            return tmp.pos;
        return pos;
    }

    /**
     * Returns the cached temporary speed value.
     * @return the cached temporary speed value
     */
    public Double getSpeed() {
        if (tmp != null)
            return tmp.speed;
        return speed;
    }

    /**
     * Returns the cached temporary elevation value.
     * @return the cached temporary elevation value
     */
    public Double getElevation() {
        if (tmp != null)
            return tmp.elevation;
        return elevation;
    }

    /**
     * Returns the cached temporary GPS time value.
     * @return the cached temporary GPS time value
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
        return exifImgDir;
    }

    public boolean hasThumbnail() {
        return thumbnail != null;
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
        setPos(new CachedLatLon(pos));
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

    public void setExifImgDir(double exifDir) {
        this.exifImgDir = exifDir;
    }

    @Override
    public ImageEntry clone() {
        Object c;
        try {
            c = super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return (ImageEntry) c;
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
     * Make a fresh copy and save it in the temporary variable.
     */
    public void cleanTmp() {
        tmp = clone();
        tmp.setPos(null);
        tmp.tmp = null;
    }

    /**
     * Copy the values from the temporary variable to the main instance.
     */
    public void applyTmp() {
        if (tmp != null) {
            pos = tmp.pos;
            speed = tmp.speed;
            elevation = tmp.elevation;
            gpsTime = tmp.gpsTime;
            tmp = null;
        }
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
}
