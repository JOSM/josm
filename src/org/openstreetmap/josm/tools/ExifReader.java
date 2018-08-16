// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.date.DateUtils;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

/**
 * Read out EXIF information from a JPEG file
 * @author Imi
 * @since 99
 */
public final class ExifReader {

    private ExifReader() {
        // Hide default constructor for utils classes
    }

    /**
     * Returns the date/time from the given JPEG file.
     * @param filename The JPEG file to read
     * @return The date/time read in the EXIF section, or {@code null} if not found
     */
    public static Date readTime(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            return readTime(metadata);
        } catch (JpegProcessingException | IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the date/time from the given JPEG file.
     * @param metadata The EXIF metadata
     * @return The date/time read in the EXIF section, or {@code null} if not found
     * @since 11745
     */
    public static Date readTime(Metadata metadata) {
        try {
            String dateTimeOrig = null;
            String dateTime = null;
            String dateTimeDig = null;
            String subSecOrig = null;
            String subSec = null;
            String subSecDig = null;
            // The date fields are preferred in this order: DATETIME_ORIGINAL
            // (0x9003), DATETIME (0x0132), DATETIME_DIGITIZED (0x9004).  Some
            // cameras store the fields in the wrong directory, so all
            // directories are searched.  Assume that the order of the fields
            // in the directories is random.
            for (Directory dirIt : metadata.getDirectories()) {
                if (!(dirIt instanceof ExifDirectoryBase)) {
                    continue;
                }
                for (Tag tag : dirIt.getTags()) {
                    if (tag.getTagType() == ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL /* 0x9003 */ &&
                            !tag.getDescription().matches("\\[[0-9]+ .+\\]")) {
                        dateTimeOrig = tag.getDescription();
                    } else if (tag.getTagType() == ExifIFD0Directory.TAG_DATETIME /* 0x0132 */) {
                        dateTime = tag.getDescription();
                    } else if (tag.getTagType() == ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED /* 0x9004 */) {
                        dateTimeDig = tag.getDescription();
                    } else if (tag.getTagType() == ExifSubIFDDirectory.TAG_SUBSECOND_TIME_ORIGINAL /* 0x9291 */) {
                        subSecOrig = tag.getDescription();
                    } else if (tag.getTagType() == ExifSubIFDDirectory.TAG_SUBSECOND_TIME /* 0x9290 */) {
                        subSec = tag.getDescription();
                    } else if (tag.getTagType() == ExifSubIFDDirectory.TAG_SUBSECOND_TIME_DIGITIZED /* 0x9292 */) {
                        subSecDig = tag.getDescription();
                    }
                }
            }
            String dateStr = null;
            String subSeconds = null;
            if (dateTimeOrig != null) {
                // prefer TAG_DATETIME_ORIGINAL
                dateStr = dateTimeOrig;
                subSeconds = subSecOrig;
            } else if (dateTime != null) {
                // TAG_DATETIME is second choice, see #14209
                dateStr = dateTime;
                subSeconds = subSec;
            } else if (dateTimeDig != null) {
                dateStr = dateTimeDig;
                subSeconds = subSecDig;
            }
            if (dateStr != null) {
                dateStr = dateStr.replace('/', ':'); // workaround for HTC Sensation bug, see #7228
                final Date date = DateUtils.fromString(dateStr);
                if (subSeconds != null) {
                    try {
                        date.setTime(date.getTime() + (long) (TimeUnit.SECONDS.toMillis(1) * Double.parseDouble("0." + subSeconds)));
                    } catch (NumberFormatException e) {
                        Logging.warn("Failed parsing sub seconds from [{0}]", subSeconds);
                        Logging.warn(e);
                    }
                }
                return date;
            }
        } catch (UncheckedParseException | DateTimeException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the image orientation of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The image orientation as an {@code int}. Default value is 1. Possible values are listed in EXIF spec as follows:<br><ol>
     * <li>The 0th row is at the visual top of the image, and the 0th column is the visual left-hand side.</li>
     * <li>The 0th row is at the visual top of the image, and the 0th column is the visual right-hand side.</li>
     * <li>The 0th row is at the visual bottom of the image, and the 0th column is the visual right-hand side.</li>
     * <li>The 0th row is at the visual bottom of the image, and the 0th column is the visual left-hand side.</li>
     * <li>The 0th row is the visual left-hand side of the image, and the 0th column is the visual top.</li>
     * <li>The 0th row is the visual right-hand side of the image, and the 0th column is the visual top.</li>
     * <li>The 0th row is the visual right-hand side of the image, and the 0th column is the visual bottom.</li>
     * <li>The 0th row is the visual left-hand side of the image, and the 0th column is the visual bottom.</li></ol>
     * @see <a href="http://www.impulseadventure.com/photo/exif-orientation.html">http://www.impulseadventure.com/photo/exif-orientation.html</a>
     * @see <a href="http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto">
     * http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto</a>
     */
    public static Integer readOrientation(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            return dir == null ? null : dir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (JpegProcessingException | IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the geolocation of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The lat/lon read in the EXIF section, or {@code null} if not found
     * @since 6209
     */
    public static LatLon readLatLon(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            return readLatLon(dirGps);
        } catch (JpegProcessingException | IOException | MetadataException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the geolocation of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The lat/lon read in the EXIF section, or {@code null} if {@code dirGps} is null
     * @throws MetadataException if invalid metadata is given
     * @since 6209
     */
    public static LatLon readLatLon(GpsDirectory dirGps) throws MetadataException {
        if (dirGps != null) {
            double lat = readAxis(dirGps, GpsDirectory.TAG_LATITUDE, GpsDirectory.TAG_LATITUDE_REF, 'S');
            double lon = readAxis(dirGps, GpsDirectory.TAG_LONGITUDE, GpsDirectory.TAG_LONGITUDE_REF, 'W');
            return new LatLon(lat, lon);
        }
        return null;
    }

    /**
     * Returns the direction of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The direction of the image when it was captures (in degrees between 0.0 and 359.99),
     * or {@code null} if not found
     * @since 6209
     */
    public static Double readDirection(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            return readDirection(dirGps);
        } catch (JpegProcessingException | IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the direction of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The direction of the image when it was captured (in degrees between 0.0 and 359.99),
     * or {@code null} if missing or if {@code dirGps} is null
     * @since 6209
     */
    public static Double readDirection(GpsDirectory dirGps) {
        if (dirGps != null) {
            Rational direction = dirGps.getRational(GpsDirectory.TAG_IMG_DIRECTION);
            if (direction != null) {
                return direction.doubleValue();
            }
        }
        return null;
    }

    private static double readAxis(GpsDirectory dirGps, int gpsTag, int gpsTagRef, char cRef) throws MetadataException {
        double value;
        Rational[] components = dirGps.getRationalArray(gpsTag);
        if (components != null) {
            double deg = components[0].doubleValue();
            double min = components[1].doubleValue();
            double sec = components[2].doubleValue();

            if (Double.isNaN(deg) && Double.isNaN(min) && Double.isNaN(sec))
                throw new IllegalArgumentException("deg, min and sec are NaN");

            value = Double.isNaN(deg) ? 0 : deg + (Double.isNaN(min) ? 0 : (min / 60)) + (Double.isNaN(sec) ? 0 : (sec / 3600));

            String s = dirGps.getString(gpsTagRef);
            if (s != null && s.charAt(0) == cRef) {
                value = -value;
            }
        } else {
            // Try to read lon/lat as double value (Nonstandard, created by some cameras -> #5220)
            value = dirGps.getDouble(gpsTag);
        }
        return value;
    }

    /**
     * Returns the speed of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The speed of the camera when the image was captured (in km/h),
     *         or {@code null} if not found
     * @since 11745
     */
    public static Double readSpeed(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            return readSpeed(dirGps);
        } catch (JpegProcessingException | IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the speed of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The speed of the camera when the image was captured (in km/h),
     *         or {@code null} if missing or if {@code dirGps} is null
     * @since 11745
     */
    public static Double readSpeed(GpsDirectory dirGps) {
        if (dirGps != null) {
            Double speed = dirGps.getDoubleObject(GpsDirectory.TAG_SPEED);
            if (speed != null) {
                final String speedRef = dirGps.getString(GpsDirectory.TAG_SPEED_REF);
                if ("M".equalsIgnoreCase(speedRef)) {
                    // miles per hour
                    speed *= SystemOfMeasurement.IMPERIAL.bValue / 1000;
                } else if ("N".equalsIgnoreCase(speedRef)) {
                    // knots == nautical miles per hour
                    speed *= SystemOfMeasurement.NAUTICAL_MILE.bValue / 1000;
                }
                // default is K (km/h)
                return speed;
            }
        }
        return null;
    }

    /**
     * Returns the elevation of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The elevation of the camera when the image was captured (in m),
     *         or {@code null} if not found
     * @since 11745
     */
    public static Double readElevation(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            return readElevation(dirGps);
        } catch (JpegProcessingException | IOException e) {
            Logging.error(e);
        }
        return null;
    }

    /**
     * Returns the elevation of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The elevation of the camera when the image was captured (in m),
     *         or {@code null} if missing or if {@code dirGps} is null
     * @since 11745
     */
    public static Double readElevation(GpsDirectory dirGps) {
        if (dirGps != null) {
            Double ele = dirGps.getDoubleObject(GpsDirectory.TAG_ALTITUDE);
            if (ele != null) {
                final Integer d = dirGps.getInteger(GpsDirectory.TAG_ALTITUDE_REF);
                if (d != null && d.intValue() == 1) {
                    ele *= -1;
                }
                return ele;
            }
        }
        return null;
    }

    /**
     * Returns a Transform that fixes the image orientation.
     *
     * Only orientation 1, 3, 6 and 8 are supported. Everything else is treated as 1.
     * @param orientation the exif-orientation of the image
     * @param width the original width of the image
     * @param height the original height of the image
     * @return a transform that rotates the image, so it is upright
     */
    public static AffineTransform getRestoreOrientationTransform(final int orientation, final int width, final int height) {
        final int q;
        final double ax, ay;
        switch (orientation) {
        case 8:
            q = -1;
            ax = width / 2d;
            ay = width / 2d;
            break;
        case 3:
            q = 2;
            ax = width / 2d;
            ay = height / 2d;
            break;
        case 6:
            q = 1;
            ax = height / 2d;
            ay = height / 2d;
            break;
        default:
            q = 0;
            ax = 0;
            ay = 0;
        }
        return AffineTransform.getQuadrantRotateInstance(q, ax, ay);
    }

    /**
     * Check, if the given orientation switches width and height of the image.
     * E.g. 90 degree rotation
     *
     * Only orientation 1, 3, 6 and 8 are supported. Everything else is treated
     * as 1.
     * @param orientation the exif-orientation of the image
     * @return true, if it switches width and height
     */
    public static boolean orientationSwitchesDimensions(int orientation) {
        return orientation == 6 || orientation == 8;
    }

    /**
     * Check, if the given orientation requires any correction to the image.
     *
     * Only orientation 1, 3, 6 and 8 are supported. Everything else is treated
     * as 1.
     * @param orientation the exif-orientation of the image
     * @return true, unless the orientation value is 1 or unsupported.
     */
    public static boolean orientationNeedsCorrection(int orientation) {
        return orientation == 3 || orientation == 6 || orientation == 8;
    }
}
