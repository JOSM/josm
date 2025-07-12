// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.imageio.IIOParam;

import org.openstreetmap.josm.data.IQuadBucketType;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.gui.layer.geoimage.ImageMetadata;

/**
 * Stores info about each image
 * @since 14205 (extracted from gui.layer.geoimage.ImageEntry)
 */
public class GpxImageEntry implements Comparable<GpxImageEntry>, IQuadBucketType, ImageMetadata {
    private File file;
    private Integer exifOrientation;
    private LatLon exifCoor;
    private Double exifImgDir;
    private Double exifGpsTrack;
    private Double exifHPosErr;
    private Double exifGpsDop;
    private Instant exifTime;
    private Projections cameraProjection = Projections.UNKNOWN;
    /**
     * Flag isNewGpsData indicates that the GPS data of the image is new or has changed.
     * GPS data includes the position, speed, elevation, time (e.g. as extracted from the GPS track).
     * The flag can used to decide for which image file the EXIF GPS data is (re-)written.
     */
    private boolean isNewGpsData;
    /** Temporary source of GPS time if not correlated with GPX track. */
    private Instant exifGpsTime;

    private String iptcCaption;
    private String iptcHeadline;
    private List<String> iptcKeywords;
    private String iptcObjectName;

    /**
     * The following values are computed from the correlation with the gpx track
     * or extracted from the image EXIF data.
     */
    private CachedLatLon pos;
    /** Speed in kilometer per hour */
    private Double speed;
    /** Elevation (altitude) in meters */
    private Double elevation;
    /** GPS Differential mode */
    private Integer gpsDiffMode;
    /** GPS Measure mode */
    private Integer gps2d3dMode;
    /** GPS Datum */
    private String exifGpsDatum;
    /** GPS processing method */
    private String exifGpsProcMethod;
    /** The time after correlation with a gpx track */
    private Instant gpsTime;

    private int width;
    private int height;

    /**
     * When the correlation dialog is open, we like to show the image position
     * for the current time offset on the map in real time.
     * On the other hand, when the user aborts this operation, the old values
     * should be restored. We have a temporary copy, that overrides
     * the normal values if it is not null. (This may be not the most elegant
     * solution for this, but it works.)
     */
    private GpxImageEntry tmp;

    /**
     * Constructs a new {@code GpxImageEntry}.
     */
    public GpxImageEntry() {}

    /**
     * Constructs a new {@code GpxImageEntry} from an existing instance.
     * @param other existing instance
     * @since 14624
     */
    public GpxImageEntry(GpxImageEntry other) {
        file = other.file;
        exifOrientation = other.exifOrientation;
        exifCoor = other.exifCoor;
        exifImgDir = other.exifImgDir;
        exifGpsTrack = other.exifGpsTrack;
        exifHPosErr = other.exifHPosErr;
        gpsDiffMode = other.gpsDiffMode;
        gps2d3dMode = other.gps2d3dMode;
        exifGpsDop = other.exifGpsDop;
        exifGpsDatum = other.exifGpsDatum;
        exifGpsProcMethod = other.exifGpsProcMethod;
        exifTime = other.exifTime;
        isNewGpsData = other.isNewGpsData;
        exifGpsTime = other.exifGpsTime;
        pos = other.pos;
        speed = other.speed;
        elevation = other.elevation;
        gpsTime = other.gpsTime;
        width = other.width;
        height = other.height;
        tmp = other.tmp;
    }

    /**
     * Constructs a new {@code GpxImageEntry}.
     * @param file Path to image file on disk
     */
    public GpxImageEntry(File file) {
        setFile(file);
    }

    @Override
    public URI getImageURI() {
        return this.getFile().toURI();
    }

    /**
     * Returns width of the image this GpxImageEntry represents.
     * @return width of the image this GpxImageEntry represents
     * @since 13220
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Returns height of the image this GpxImageEntry represents.
     * @return height of the image this GpxImageEntry represents
     * @since 13220
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Returns the position value. The position value from the temporary copy
     * is returned if that copy exists.
     * @return the position value
     */
    @Override
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
    @Override
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
    @Override
    public Double getElevation() {
        if (tmp != null)
            return tmp.elevation;
        return elevation;
    }

    /**
     * Return the GPS Differential mode value. The GPS Differential mode value from the temporary
     * copy is returned if that copy exists. 
     * @return the differential mode value
     * @since 19387
     */
    @Override
    public Integer getGpsDiffMode() {
        if (tmp != null)
            return tmp.gpsDiffMode;
        return gpsDiffMode;
    }

    /**
     * Return the GPS 2d or 3d mode value. The GPS mode value form the temporary
     * copy is returned if that copy exists. 
     * @return the GPS 2d/3d mode value
     * @since 19387
     */
    @Override
    public Integer getGps2d3dMode() {
        if (tmp != null)
            return tmp.gps2d3dMode;
        return gps2d3dMode;
    }

    /**
     * Return the GPS DOP value. The GPS DOP value from the temporary
     * copy is returned if that copy exists. 
     * @return the DOP value
     * @since 19387
     */
    @Override
    public Double getExifGpsDop() {
        if (tmp != null)
            return tmp.exifGpsDop;
        return exifGpsDop;
    }

    /**
     * Return the EXIF GPS coordinates datum value.
     * @return The datum value
     * @since 19387
     */
    @Override
    public String getExifGpsDatum() {
        if (tmp != null)
            return tmp.exifGpsDatum;
        return exifGpsDatum;
    }

    /**
     * Return the EXIF GPS processing method string
     * @return the processing method string
     * @since 19387
     */
    @Override
    public String getExifGpsProcMethod() {
        if (tmp != null)
            return tmp.exifGpsProcMethod;
        return exifGpsProcMethod;
    }

    /**
     * Returns the GPS time value. The GPS time value from the temporary copy
     * is returned if that copy exists.
     * @return the GPS time value
     */
    @Override
    public Instant getGpsInstant() {
        return tmp != null ? tmp.gpsTime : gpsTime;
    }

    /**
     * Convenient way to determine if this entry has a GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a GPS time
     * @since 6450
     */
    @Override
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
     * Returns a display name for this entry
     * @return a display name for this entry
     */
    @Override
    public String getDisplayName() {
        return file == null ? "" : file.getName();
    }

    /**
     * Returns EXIF orientation
     * @return EXIF orientation
     */
    @Override
    public Integer getExifOrientation() {
        return exifOrientation != null ? exifOrientation : 1;
    }

    /**
     * Returns EXIF time
     * @return EXIF time
     * @since 17715
     */
    @Override
    public Instant getExifInstant() {
        return exifTime;
    }

    /**
     * Convenient way to determine if this entry has a EXIF time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF time
     * @since 6450
     */
    @Override
    public boolean hasExifTime() {
        return exifTime != null;
    }

    /**
     * Returns the EXIF GPS time.
     * @return the EXIF GPS time
     * @since 17715
     */
    @Override
    public Instant getExifGpsInstant() {
        return exifGpsTime;
    }

    /**
     * Convenient way to determine if this entry has a EXIF GPS time, without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF GPS time
     * @since 6450
     */
    @Override
    public boolean hasExifGpsTime() {
        return exifGpsTime != null;
    }

    /**
     * Return the time value selected with the parameter.
     * @param timeSource the wanted time value, exifCamTime or exifGpsTime
     * @return exifInstant or exifGpsInstant value
     * @since 19426
     */
    @Override
    public Instant getTimeSourceInstant(TimeSource timeSource) {
        if (timeSource == TimeSource.EXIFGPSTIME)
            return getExifGpsInstant();
        else if (timeSource == TimeSource.EXIFCAMTIME)
            return getExifInstant();
        return null;
    }

    private static Date getDefensiveDate(Instant date) {
        if (date == null)
            return null;
        return Date.from(date);
    }

    @Override
    public LatLon getExifCoor() {
        return exifCoor;
    }

    @Override
    public Double getExifImgDir() {
        if (tmp != null)
            return tmp.exifImgDir;
        return exifImgDir;
    }
    
    /**
     * Convenient way to determine if this entry has a EXIF GPS track angle value,
     * without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF track angle value
     * @since 19387
     */
    @Override
    public Double getExifGpsTrack() {
        if (tmp != null)
            return tmp.exifGpsTrack;
        return exifGpsTrack;
    }

    /**
     * Convenient way to determine if this entry has a EXIF GPS horizontal positionning error value,
     * without the cost of building a defensive copy.
     * @return {@code true} if this entry has a EXIF GPS horizontal positionning error value
     * @since 19387
     */
    @Override
    public Double getExifHPosErr() {
        if (tmp != null)
            return tmp.exifHPosErr;
        return exifHPosErr;
    }

    @Override
    public Instant getLastModified() {
        return Instant.ofEpochMilli(this.getFile().lastModified());
    }

    /**
     * Sets the width of this GpxImageEntry.
     * @param width set the width of this GpxImageEntry
     * @since 13220
     */
    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of this GpxImageEntry.
     * @param height set the height of this GpxImageEntry
     * @since 13220
     */
    @Override
    public void setHeight(int height) {
        this.height = height;
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

    @Override
    public void setPos(ILatLon pos) {
        if (pos instanceof CachedLatLon) {
            this.setPos((CachedLatLon) pos);
        } else if (pos instanceof LatLon) {
            this.setPos((LatLon) pos);
        } else if (pos != null) {
            this.setPos(new LatLon(pos));
        } else {
            this.setPos(null);
        }
    }

    /**
     * Sets the speed.
     * @param speed speed
     */
    @Override
    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    /**
     * Sets the elevation.
     * @param elevation elevation
     */
    @Override
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    /**
     * Sets GPS Differential mode.
     * @param gpsDiffMode GPS Differential mode
     * @since 19387
     */
    @Override
    public void setGpsDiffMode(Integer gpsDiffMode) {
        this.gpsDiffMode = gpsDiffMode;
    }

    /**
     * Sets GPS 2d/3d mode.
     * @param gps2d3dMode GPS 2d/3d mode value
     * @since 19387
     */
    @Override
    public void setGps2d3dMode(Integer gps2d3dMode) {
        this.gps2d3dMode = gps2d3dMode;
    }

    /**
     * Sets GPS DOP value.
     * @param exifGpsDop GPS DOP value
     * @since 19387
     */
    @Override
    public void setExifGpsDop(Double exifGpsDop) {
        this.exifGpsDop = exifGpsDop;
    }

    /**
     * Sets the GPS Datum.
     * @param exifGpsDatum GPS Datum
     * @since 19387
     */
    @Override
    public void setExifGpsDatum(String exifGpsDatum) {
        this.exifGpsDatum = exifGpsDatum;
    }

    /**
     * Sets the GPS Processing Method.
     * @param exifGpsProcMethod GPS Processing Method
     * @since 19387
     */
    @Override
    public void setExifGpsProcMethod(String exifGpsProcMethod) {
        this.exifGpsProcMethod = exifGpsProcMethod;
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
    @Override
    public void setExifOrientation(Integer exifOrientation) {
        this.exifOrientation = exifOrientation;
    }

    /**
     * Sets EXIF time.
     * @param exifTime EXIF time
     * @since 17715
     */
    @Override
    public void setExifTime(Instant exifTime) {
        this.exifTime = exifTime;
    }

    /**
     * Sets the EXIF GPS time.
     * @param exifGpsTime the EXIF GPS time
     * @since 17715
     */
    @Override
    public void setExifGpsTime(Instant exifGpsTime) {
        this.exifGpsTime = exifGpsTime;
    }

    /**
     * Sets the GPS time.
     * @param gpsTime the GPS time
     * @since 17715
     */
    @Override
    public void setGpsTime(Instant gpsTime) {
        this.gpsTime = gpsTime;
    }

    /**
     * Sets the EXIF coordinate.
     * @param exifCoor the coordinate
     */
    public void setExifCoor(LatLon exifCoor) {
        this.exifCoor = exifCoor;
    }

    @Override
    public void setExifCoor(ILatLon exifCoor) {
        if (exifCoor instanceof LatLon) {
            this.exifCoor = (LatLon) exifCoor;
        } else if (exifCoor != null) {
            this.exifCoor = new LatLon(exifCoor);
        } else {
            this.exifCoor = null;
        }
    }

    @Override
    public void setExifImgDir(Double exifDir) {
        this.exifImgDir = exifDir;
    }

    /**
     * Sets the EXIF GPS track (move direction angle)
     * @param exifGpsTrack the EXIF GPS track angle
     * @since 19387
     */
    @Override
    public void setExifGpsTrack(Double exifGpsTrack) {
        this.exifGpsTrack = exifGpsTrack;
    }

    /**
     * Sets the EXIF horizontal positioning error
     * @param exifHPosErr the EXIF horizontal positionning error
     * @since 19387
     */
    @Override
    public void setExifHPosErr(Double exifHPosErr) {
        this.exifHPosErr = exifHPosErr;
    }

    /**
     * Sets the IPTC caption.
     * @param iptcCaption the IPTC caption
     * @since 15219
     */
    @Override
    public void setIptcCaption(String iptcCaption) {
        this.iptcCaption = iptcCaption;
    }

    /**
     * Sets the IPTC headline.
     * @param iptcHeadline the IPTC headline
     * @since 15219
     */
    @Override
    public void setIptcHeadline(String iptcHeadline) {
        this.iptcHeadline = iptcHeadline;
    }

    /**
     * Sets the IPTC keywords.
     * @param iptcKeywords the IPTC keywords
     * @since 15219
     */
    @Override
    public void setIptcKeywords(List<String> iptcKeywords) {
        this.iptcKeywords = iptcKeywords;
    }

    /**
     * Sets the IPTC object name.
     * @param iptcObjectName the IPTC object name
     * @since 15219
     */
    @Override
    public void setIptcObjectName(String iptcObjectName) {
        this.iptcObjectName = iptcObjectName;
    }

    /**
     * Returns the IPTC caption.
     * @return the IPTC caption
     * @since 15219
     */
    @Override
    public String getIptcCaption() {
        return iptcCaption;
    }

    /**
     * Returns the IPTC headline.
     * @return the IPTC headline
     * @since 15219
     */
    @Override
    public String getIptcHeadline() {
        return iptcHeadline;
    }

    /**
     * Returns the IPTC keywords.
     * @return the IPTC keywords
     * @since 15219
     */
    @Override
    public List<String> getIptcKeywords() {
        return iptcKeywords;
    }

    /**
     * Returns the IPTC object name.
     * @return the IPTC object name
     * @since 15219
     */
    @Override
    public String getIptcObjectName() {
        return iptcObjectName;
    }

    @Override
    public int compareTo(GpxImageEntry image) {
        if (exifTime != null && image.exifTime != null)
            return exifTime.compareTo(image.exifTime);
        else if (exifTime == null && image.exifTime == null)
            return 0;
        else if (exifTime == null)
            return -1;
        else
            return 1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width, isNewGpsData,
            elevation, exifCoor, exifGpsTime, exifImgDir, exifGpsTrack, exifHPosErr,
            exifGpsDop, gpsDiffMode, gps2d3dMode, exifGpsDatum, exifGpsProcMethod,
            exifOrientation, exifTime, iptcCaption, iptcHeadline, iptcKeywords,
            iptcObjectName, file, gpsTime, pos, speed, tmp, cameraProjection);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        GpxImageEntry other = (GpxImageEntry) obj;
        return height == other.height
            && width == other.width
            && isNewGpsData == other.isNewGpsData
            && Objects.equals(elevation, other.elevation)
            && Objects.equals(exifCoor, other.exifCoor)
            && Objects.equals(exifGpsTime, other.exifGpsTime)
            && Objects.equals(exifImgDir, other.exifImgDir)
            && Objects.equals(exifGpsTrack, other.exifGpsTrack)
            && Objects.equals(exifHPosErr, other.exifHPosErr)
            && Objects.equals(gpsDiffMode, other.gpsDiffMode)
            && Objects.equals(gps2d3dMode, other.gps2d3dMode)
            && Objects.equals(exifGpsDop, other.exifGpsDop)
            && Objects.equals(exifGpsDatum, other.exifGpsDatum)
            && Objects.equals(exifGpsProcMethod, other.exifGpsProcMethod)
            && Objects.equals(exifOrientation, other.exifOrientation)
            && Objects.equals(exifTime, other.exifTime)
            && Objects.equals(iptcCaption, other.iptcCaption)
            && Objects.equals(iptcHeadline, other.iptcHeadline)
            && Objects.equals(iptcKeywords, other.iptcKeywords)
            && Objects.equals(iptcObjectName, other.iptcObjectName)
            && Objects.equals(file, other.file)
            && Objects.equals(gpsTime, other.gpsTime)
            && Objects.equals(pos, other.pos)
            && Objects.equals(speed, other.speed)
            && Objects.equals(tmp, other.tmp)
            && cameraProjection == other.cameraProjection;
    }

    /**
     * Make a fresh copy and save it in the temporary variable. Use
     * {@link #applyTmp()} or {@link #discardTmp()} if the temporary variable
     * is not needed anymore.
     * @return the fresh copy.
     */
    public GpxImageEntry createTmp() {
        tmp = new GpxImageEntry(this);
        tmp.tmp = null;
        return tmp;
    }

    /**
     * Get temporary variable that is used for real time parameter
     * adjustments. The temporary variable is created if it does not exist
     * yet. Use {@link #applyTmp()} or {@link #discardTmp()} if the temporary
     * variable is not needed anymore.
     * @return temporary variable
     */
    public GpxImageEntry getTmp() {
        if (tmp == null) {
            createTmp();
        }
        return tmp;
    }

    /**
     * Copy the values from the temporary variable to the main instance. The
     * temporary variable is deleted.
     * @see #discardTmp()
     * @since 19387 exifGpsTrack, exifHPosErr, gpsDiffMode, gps2d3dMode, exifGpsDop, exifGpsDatum, exifGpsProcMethod added
     */
    public void applyTmp() {
        if (tmp != null) {
            pos = tmp.pos;
            speed = tmp.speed;
            elevation = tmp.elevation;
            gpsTime = tmp.gpsTime;
            exifImgDir = tmp.exifImgDir;
            exifGpsTrack = tmp.exifGpsTrack;
            exifHPosErr = tmp.exifHPosErr;
            gpsDiffMode = tmp.gpsDiffMode;
            gps2d3dMode = tmp.gps2d3dMode;
            exifGpsDop = tmp.exifGpsDop;
            exifGpsDatum = tmp.exifGpsDatum;
            exifGpsProcMethod = tmp.exifGpsProcMethod;
            isNewGpsData = isNewGpsData || tmp.isNewGpsData;
            tmp = null;
        }
        tmpUpdated();
    }

    /**
     * Delete the temporary variable. Temporary modifications are lost.
     * @see #applyTmp()
     */
    public void discardTmp() {
        tmp = null;
        tmpUpdated();
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
     * Indicate that the temporary copy has been updated. Mostly used to prevent UI issues.
     * By default, this is a no-op. Override when needed in subclasses.
     * @since 17579
     */
    protected void tmpUpdated() {
        // No-op by default
    }

    @Override
    public BBox getBBox() {
        // new BBox(LatLon) is null safe.
        // Use `getPos` instead of `getExifCoor` since the image may be correlated against a GPX track
        return new BBox(this.getPos());
    }

    /**
     * Remove the flag that indicates new GPS data.
     * The flag is cleared by a new GPS data consumer.
     */
    public void unflagNewGpsData() {
        isNewGpsData = false;
    }

    /**
     * Queries whether the GPS data changed. The flag value from the temporary
     * copy is returned if that copy exists.
     * @return {@code true} if GPS data changed, {@code false} otherwise
     * @since 6392
     */
    public boolean hasNewGpsData() {
        if (tmp != null)
            return tmp.isNewGpsData;
        return isNewGpsData;
    }

    /**
     * Extract GPS metadata from image EXIF. Has no effect if the image file is not set
     *
     * If successful, fills in the LatLon, speed, elevation, image direction, and other attributes
     * @since 9270
     */
    @Override
    public void extractExif() {
        ImageMetadata.super.extractExif();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(this.getFile().toPath());
    }

    /**
     * Reads the image represented by this entry in the given target dimension.
     * @param target the desired dimension used for {@linkplain IIOParam#setSourceSubsampling subsampling} or {@code null}
     * @return the read image, or {@code null}
     * @throws IOException if any I/O error occurs
     * @since 18246
     */
    public BufferedImage read(Dimension target) throws IOException {
        throw new UnsupportedOperationException("read not implemented for " + this.getClass().getSimpleName());
    }

    /**
     * Get the projection type for this entry
     * @return The projection type
     * @since 18246
     */
    @Override
    public Projections getProjectionType() {
        return this.cameraProjection;
    }

    @Override
    public void setProjectionType(Projections newProjection) {
        this.cameraProjection = newProjection;
    }

    /**
     * Returns a {@link WayPoint} representation of this GPX image entry.
     * @return a {@code WayPoint} representation of this GPX image entry (containing position, instant and elevation)
     * @since 18065
     */
    public WayPoint asWayPoint() {
        CachedLatLon position = getPos();
        WayPoint wpt = null;
        if (position != null) {
            wpt = new WayPoint(position);
            wpt.setInstant(exifTime);
            Double ele = getElevation();
            if (ele != null) {
                wpt.put(GpxConstants.PT_ELE, ele.toString());
            }
        }
        return wpt;
    }
}
