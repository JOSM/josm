// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
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
     * @throws ParseException if {@link DateParser#parse} fails to parse date/time
     */
    public static Date readTime(File filename) throws ParseException {
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(filename);
            String dateStr = null;
            OUTER:
            for (Directory dirIt : metadata.getDirectories()) {
                for (Tag tag : dirIt.getTags()) {
                    if (tag.getTagType() == ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL /* 0x9003 */) {
                        dateStr = tag.getDescription();
                        break OUTER; // prefer this tag
                    }
                    if (tag.getTagType() == ExifIFD0Directory.TAG_DATETIME /* 0x0132 */ ||
                        tag.getTagType() == ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED /* 0x9004 */) {
                        dateStr = tag.getDescription();
                    }
                }
            }
            if (dateStr != null) {
                dateStr = dateStr.replace('/', ':'); // workaround for HTC Sensation bug, see #7228
                return DateParser.parse(dateStr);
            }
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            Main.error(e);
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
     * @see <a href="http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto">http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto</a>
     */
    public static Integer readOrientation(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final Directory dir = metadata.getDirectory(ExifIFD0Directory.class);
            return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (JpegProcessingException e) {
            Main.error(e);
        } catch (MetadataException e) {
            Main.error(e);
        } catch (IOException e) {
            Main.error(e);
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
            final GpsDirectory dirGps = metadata.getDirectory(GpsDirectory.class);
            return readLatLon(dirGps);
        } catch (JpegProcessingException e) {
            Main.error(e);
        } catch (IOException e) {
            Main.error(e);
        } catch (MetadataException e) {
            Main.error(e);
        }
        return null;
    }

    /**
     * Returns the geolocation of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The lat/lon read in the EXIF section, or {@code null} if {@code dirGps} is null
     * @throws MetadataException
     * @since 6209
     */
    public static LatLon readLatLon(GpsDirectory dirGps) throws MetadataException {
        if (dirGps != null) {
            double lat = readAxis(dirGps, GpsDirectory.TAG_GPS_LATITUDE, GpsDirectory.TAG_GPS_LATITUDE_REF, 'S');
            double lon = readAxis(dirGps, GpsDirectory.TAG_GPS_LONGITUDE, GpsDirectory.TAG_GPS_LONGITUDE_REF, 'W');
            return new LatLon(lat, lon);
        }
        return null;
    }

    /**
     * Returns the direction of the given JPEG file.
     * @param filename The JPEG file to read
     * @return The direction of the image when it was captures (in degrees between 0.0 and 359.99), or {@code null} if missing or if {@code dirGps} is null
     * @since 6209
     */
    public static Double readDirection(File filename) {
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final GpsDirectory dirGps = metadata.getDirectory(GpsDirectory.class);
            return readDirection(dirGps);
        } catch (JpegProcessingException e) {
            Main.error(e);
        } catch (IOException e) {
            Main.error(e);
        }
        return null;
    }

    /**
     * Returns the direction of the given EXIF GPS directory.
     * @param dirGps The EXIF GPS directory
     * @return The direction of the image when it was captures (in degrees between 0.0 and 359.99), or {@code null} if missing or if {@code dirGps} is null
     * @since 6209
     */
    public static Double readDirection(GpsDirectory dirGps) {
        if (dirGps != null) {
            Rational direction = dirGps.getRational(GpsDirectory.TAG_GPS_IMG_DIRECTION);
            if (direction != null) {
                return direction.doubleValue();
            }
        }
        return null;
    }

    private static double readAxis(GpsDirectory dirGps, int gpsTag, int gpsTagRef, char cRef) throws MetadataException  {
        double value;
        Rational[] components = dirGps.getRationalArray(gpsTag);
        if (components != null) {
            double deg = components[0].doubleValue();
            double min = components[1].doubleValue();
            double sec = components[2].doubleValue();

            if (Double.isNaN(deg) && Double.isNaN(min) && Double.isNaN(sec))
                throw new IllegalArgumentException();

            value = (Double.isNaN(deg) ? 0 : deg + (Double.isNaN(min) ? 0 : (min / 60)) + (Double.isNaN(sec) ? 0 : (sec / 3600)));

            if (dirGps.getString(gpsTagRef).charAt(0) == cRef) {
                value = -value;
            }
        } else {
            // Try to read lon/lat as double value (Nonstandard, created by some cameras -> #5220)
            value = dirGps.getDouble(gpsTag);
        }
        return value;
    }
}
