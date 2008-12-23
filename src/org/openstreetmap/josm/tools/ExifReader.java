// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Read out exif file information from a jpeg file
 * @author Imi
 */
public class ExifReader {

    @SuppressWarnings("unchecked") public static Date readTime(File filename) throws ParseException {
        Date date = null;
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(filename);
            for (Iterator<Directory> dirIt = metadata.getDirectoryIterator(); dirIt.hasNext();) {
                for (Iterator<Tag> tagIt = dirIt.next().getTagIterator(); tagIt.hasNext();) {
                    Tag tag = tagIt.next();
                    if (tag.getTagType() == 0x9003)
                        return DateParser.parse(tag.getDescription());
                    if (tag.getTagType() == 0x132 || tag.getTagType() == 0x9004)
                        date = DateParser.parse(tag.getDescription());
                }
            }
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }
}
