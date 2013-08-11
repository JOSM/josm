// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * Read out exif file information from a jpeg file
 * @author Imi
 */
public class ExifReader {

    @SuppressWarnings("unchecked") public static Date readTime(File filename) throws ParseException {
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
            e.printStackTrace();
        }
        return null;
    }

    public static Integer readOrientation(File filename) throws ParseException {
        Integer orientation = null;
        try {
            final Metadata metadata = JpegMetadataReader.readMetadata(filename);
            final Directory dir = metadata.getDirectory(ExifIFD0Directory.class);
            orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (JpegProcessingException e) {
            e.printStackTrace();
        } catch (MetadataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientation;
    }

}
