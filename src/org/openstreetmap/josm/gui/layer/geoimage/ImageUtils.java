// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.imaging.png.PngMetadataReader;
import com.drew.imaging.png.PngProcessingException;
import com.drew.imaging.tiff.TiffMetadataReader;
import com.drew.imaging.tiff.TiffProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Image utilities
 * @since 18592
 */
public final class ImageUtils {
    private ImageUtils() {
        // Hide constructor
    }

    /**
     * Rotate an image, if needed
     * @param img The image to rotate
     * @param exifOrientation The exif orientation
     * @return The rotated image or the original
     */
    public static BufferedImage applyExifRotation(BufferedImage img, Integer exifOrientation) {
        if (exifOrientation == null || !ExifReader.orientationNeedsCorrection(exifOrientation)) {
            return img;
        }
        boolean switchesDimensions = ExifReader.orientationSwitchesDimensions(exifOrientation);
        int width = switchesDimensions ? img.getHeight() : img.getWidth();
        int height = switchesDimensions ? img.getWidth() : img.getHeight();
        BufferedImage rotated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        AffineTransform transform = ExifReader.getRestoreOrientationTransform(exifOrientation, img.getWidth(), img.getHeight());
        Graphics2D g = rotated.createGraphics();
        g.drawImage(img, transform, null);
        g.dispose();
        return rotated;
    }

    /**
     * Common subsampling method
     * @param reader The image reader
     * @param target The target area
     * @return The sampling parameters
     */
    public static ImageReadParam withSubsampling(ImageReader reader, Dimension target) {
        try {
            ImageReadParam param = reader.getDefaultReadParam();
            Dimension source = new Dimension(reader.getWidth(0), reader.getHeight(0));
            if (source.getWidth() > target.getWidth() || source.getHeight() > target.getHeight()) {
                int subsampling = (int) Math.floor(Math.max(
                        source.getWidth() / target.getWidth(),
                        source.getHeight() / target.getHeight()));
                param.setSourceSubsampling(subsampling, subsampling, 0, 0);
            }
            return param;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Apply exif information from an {@link InputStream}
     * @param image The image to apply information to
     * @param inputStream The input stream to read
     */
    public static void applyExif(ImageMetadata image, InputStream inputStream) {
        Metadata metadata;

        if (image == null || inputStream == null) {
            return;
        }

        metadata = getMetadata(image.getImageURI(), inputStream);
        if (metadata == null) {
            image.setExifTime(image.getLastModified());
            image.setExifCoor(null);
            image.setPos(null);
            return;
        }
        final String fn = image.getImageURI().toString();

        IptcDirectory dirIptc = metadata.getFirstDirectoryOfType(IptcDirectory.class);
        if (dirIptc != null) {
            ifNotNull(ExifReader.readCaption(dirIptc), image::setIptcCaption);
            ifNotNull(ExifReader.readHeadline(dirIptc), image::setIptcHeadline);
            ifNotNull(ExifReader.readKeywords(dirIptc), image::setIptcKeywords);
            ifNotNull(ExifReader.readObjectName(dirIptc), image::setIptcObjectName);
        }

        for (XmpDirectory xmpDirectory : metadata.getDirectoriesOfType(XmpDirectory.class)) {
            Map<String, String> properties = xmpDirectory.getXmpProperties();
            final String projectionType = "GPano:ProjectionType";
            if (properties.containsKey(projectionType)) {
                Stream.of(Projections.values()).filter(p -> p.name().equalsIgnoreCase(properties.get(projectionType)))
                        .findFirst().ifPresent(image::setProjectionType);
                break;
            }
        }

        // Changed to silently cope with no time info in exif. One case
        // of person having time that couldn't be parsed, but valid GPS info
        Instant time = null;
        try {
            time = ExifReader.readInstant(metadata);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException ex) {
            Logging.warn(ex);
        }

        if (time == null) {
            Logging.info(tr("No EXIF time in file \"{0}\". Using last modified date as timestamp.", fn));
            time = image.getLastModified(); //use lastModified time if no EXIF time present
        }
        image.setExifTime(time);

        final Directory dir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        final Directory dirExif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        final GpsDirectory dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

        try {
            if (dirExif != null && dirExif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                image.setExifOrientation(dirExif.getInt(ExifIFD0Directory.TAG_ORIENTATION));
            }
        } catch (MetadataException ex) {
            Logging.debug(ex);
        }

        try {
            if (dir != null && dir.containsTag(JpegDirectory.TAG_IMAGE_WIDTH) && dir.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                // there are cases where these do not match width and height stored in dirExif
                image.setWidth(dir.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
                image.setHeight(dir.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
            }
        } catch (MetadataException ex) {
            Logging.debug(ex);
        }

        if (dirGps == null || dirGps.getTagCount() <= 1) {
            image.setExifCoor(null);
            image.setPos(null);
            return;
        }

        ifNotNull(ExifReader.readSpeed(dirGps), image::setSpeed);
        ifNotNull(ExifReader.readElevation(dirGps), image::setElevation);

        try {
            image.setExifCoor(ExifReader.readLatLon(dirGps));
            image.setPos(image.getExifCoor());
        } catch (MetadataException | IndexOutOfBoundsException ex) { // (other exceptions, e.g. #5271)
            Logging.error("Error reading EXIF from file: " + ex);
            image.setExifCoor(null);
            image.setPos(null);
        }

        try {
            ifNotNull(ExifReader.readDirection(dirGps), image::setExifImgDir);
        } catch (IndexOutOfBoundsException ex) { // (other exceptions, e.g. #5271)
            Logging.debug(ex);
        }

        ifNotNull(dirGps.getGpsDate(), d -> image.setExifGpsTime(d.toInstant()));
    }

    private static Metadata getMetadata(URI uri, InputStream inputStream) {
        inputStream.mark(32);
        final Exception topException;
        final String fn = uri.toString();
        try {
            // try to parse metadata according to extension
            String ext = fn.substring(fn.lastIndexOf('.') + 1).toLowerCase(Locale.US);
            switch (ext) {
                case "jpg":
                case "jpeg":
                    return JpegMetadataReader.readMetadata(inputStream);
                case "tif":
                case "tiff":
                    return TiffMetadataReader.readMetadata(inputStream);
                case "png":
                    return PngMetadataReader.readMetadata(inputStream);
                default:
                    throw new NoMetadataReaderWarning(ext);
            }
        } catch (JpegProcessingException | TiffProcessingException | PngProcessingException | IOException
                 | NoMetadataReaderWarning exception) {
            //try other formats (e.g. JPEG file with .png extension)
            topException = exception;
        }
        try {
            return JpegMetadataReader.readMetadata(inputStream);
        } catch (JpegProcessingException | IOException ex1) {
            Logging.trace(ex1);
        }
        try {
            return TiffMetadataReader.readMetadata(inputStream);
        } catch (TiffProcessingException | IOException ex2) {
            Logging.trace(ex2);
        }

        try {
            return PngMetadataReader.readMetadata(inputStream);
        } catch (PngProcessingException | IOException ex3) {
            Logging.trace(ex3);
        }
        Logging.warn(topException);
        Logging.info(tr("Can''t parse metadata for file \"{0}\". Using last modified date as timestamp.", fn));
        return null;
    }

    private static class NoMetadataReaderWarning extends Exception {
        NoMetadataReaderWarning(String ext) {
            super("No metadata reader for format *." + ext);
        }
    }

    private static <T> void ifNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
