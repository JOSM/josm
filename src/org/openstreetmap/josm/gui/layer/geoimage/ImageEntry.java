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
    File file;
    Date time;
    LatLon exifCoor;

    private CachedLatLon pos;
    /** Speed in kilometer per second */
    private Double speed;
    /** Elevation (altitude) in meters */
    private Double elevation;

    Image thumbnail;

    /**
     * When the corralation dialog is open, we like to show the image position
     * for the current time offset on the map in real time.
     * On the other hand, when the user aborts this operation, the old values
     * should be restored. We have a temprary copy, that overrides
     * the normal values if it is not null. (This may be not the most elegant
     * solution for this, but it works.)
     */
    ImageEntry tmp;

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
    public void setPos(CachedLatLon pos) {
        this.pos = pos;
    }
    public void setSpeed(Double speed) {
        this.speed = speed;
    }
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }
    
    public File getFile() {
        return file;
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

    public void setCoor(LatLon latlon)
    {
        pos = new CachedLatLon(latlon);
    }

    public int compareTo(ImageEntry image) {
        if (time != null && image.time != null)
            return time.compareTo(image.time);
        else if (time == null && image.time == null)
            return 0;
        else if (time == null)
            return -1;
        else
            return 1;
    }

    public void applyTmp() {
        if (tmp != null) {
            pos = tmp.pos;
            speed = tmp.speed;
            elevation = tmp.elevation;
            tmp = null;
        }
    }
    public void cleanTmp() {
        tmp = clone();
        tmp.setPos(null);
        tmp.tmp = null;
    }

    public boolean isTagged() {
        return pos != null;
    }

    /**
     * only partial info
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
