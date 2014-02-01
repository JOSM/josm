// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.util.Date;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Stores info about each image
 */
final public class ImageEntry implements Comparable<ImageEntry>, Cloneable {
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
    private boolean isNewGpsData = false;
    /** Temporary source of GPS time if not correlated with GPX track. */
    private Date exifGpsTime = null;
    Image thumbnail;

    /**
     * The following values are computed from the correlation with the gpx track
     * or extracted from the image EXIF data.
     */
    private CachedLatLon pos;
    /** Speed in kilometer per second */
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
     * getter methods that refer to the temporary value
     */
    public CachedLatLon getPos() {
        if (tmp != null)
            return tmp.pos;
        return pos;
    }
    public Double getSpeed() {
        if (tmp != null)
            return tmp.speed;
        return speed;
    }
    public Double getElevation() {
        if (tmp != null)
            return tmp.elevation;
        return elevation;
    }

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
    public final boolean hasGpsTime() {
        return (tmp != null && tmp.gpsTime != null) || gpsTime != null; 
    }

    /**
     * other getter methods
     */
    public File getFile() {
        return file;
    }
    public Integer getExifOrientation() {
        return exifOrientation;
    }
    public Date getExifTime() {
        return getDefensiveDate(exifTime);
    }
    
    /**
     * Convenient way to determine if this entry has a EXIF time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF time
     * @since 6450
     */
    public final boolean hasExifTime() {
        return exifTime != null; 
    }
    
    /**
     * Returns the EXIF GPS time.
     * @return the EXIF GPS time
     * @since 6392
     */
    public final Date getExifGpsTime() {
        return getDefensiveDate(exifGpsTime);
    }
    
    /**
     * Convenient way to determine if this entry has a EXIF GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF GPS time
     * @since 6450
     */
    public final boolean hasExifGpsTime() {
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
     * setter methods
     */
    public void setPos(CachedLatLon pos) {
        this.pos = pos;
    }
    public void setPos(LatLon pos) {
        this.pos = new CachedLatLon(pos);
    }
    public void setSpeed(Double speed) {
        this.speed = speed;
    }
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }
    public void setFile(File file) {
        this.file = file;
    }
    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }
    public void setExifTime(Date exifTime) {
        this.exifTime = getDefensiveDate(exifTime);
    }
    
    /**
     * Sets the EXIF GPS time.
     * @param exifGpsTime the EXIF GPS time
     * @since 6392
     */
    public final void setExifGpsTime(Date exifGpsTime) {
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
     */
    public boolean isTagged() {
        return pos != null;
    }

    /**
     * String representation. (only partial info)
     */
    @Override
    public String toString() {
        String result = file.getName()+": "+
        "pos = "+pos+" | "+
        "exifCoor = "+exifCoor+" | "+
        (tmp == null ? " tmp==null" :
            " [tmp] pos = "+tmp.pos+"");
        return result;
    }

    /**
     * Indicates that the image has new GPS data. 
     * That flag is used e.g. by the photo_geotagging plugin to decide for which image
     * file the EXIF GPS data needs to be (re-)written.
     * @since 6392
     */
    public void flagNewGpsData() {
        isNewGpsData = true;
        // We need to set the GPS time to tell the system (mainly the photo_geotagging plug-in) 
        // that the GPS data has changed. Check for existing GPS time and take EXIF time otherwise.
        // This can be removed once isNewGpsData is used instead of the GPS time.
        if (gpsTime == null) {
            Date time = getExifGpsTime();
            if (time == null) {
                time = getExifTime();
                if (time == null) {
                    // Time still not set, take the current time.
                    time = new Date();
                }
            }
            gpsTime = time;
        }
        if (tmp != null && !tmp.hasGpsTime()) {
            // tmp.gpsTime overrides gpsTime, so we set it too.
            tmp.gpsTime = gpsTime;
        }
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
