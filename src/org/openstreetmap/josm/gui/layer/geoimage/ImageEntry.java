// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Image;
import java.io.File;
import java.util.Date;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/*
 * Stores info about each image
 */

final public class ImageEntry implements Comparable<ImageEntry>, Cloneable {
    private File file;
    private LatLon exifCoor;
    private Double exifImgDir;
    private Date exifTime;
    Image thumbnail;

    /** The following values are computed from the correlation with the gpx track */
    private CachedLatLon pos;
    /** Speed in kilometer per second */
    private Double speed;
    /** Elevation (altitude) in meters */
    private Double elevation;
    /** The time after correlation with a gpx track */
    private Date gpsTime;

    /**
     * When the corralation dialog is open, we like to show the image position
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
            return tmp.gpsTime;
        return gpsTime;
    }

    /**
     * other getter methods
     */
    public File getFile() {
        return file;
    }
    public Date getExifTime() {
        return exifTime;
    }
    LatLon getExifCoor() {
        return exifCoor;
    }
    public Double getExifImgDir() {
        return exifImgDir;
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
    void setFile(File file) {
        this.file = file;
    }
    void setExifTime(Date exifTime) {
        this.exifTime = exifTime;
    }
    void setGpsTime(Date gpsTime) {
        this.gpsTime = gpsTime;
    }
    void setExifCoor(LatLon exifCoor) {
        this.exifCoor = exifCoor;
    }
    void setExifImgDir(double exifDir) {
        this.exifImgDir = exifDir;
    }

    @Override
    public ImageEntry clone() {
        Object c;
        try {
            c = super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException();
        }
        return (ImageEntry) c;
    }

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
}
