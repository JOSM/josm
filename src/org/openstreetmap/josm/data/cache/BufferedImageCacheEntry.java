// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;


/**
 * Cache Entry that has methods to get the BufferedImage, that will be cached along in memory
 * but will be not serialized when saved to the disk (to avoid duplication of data)
 * @author Wiktor Niesiobędzki
 *
 */
public class BufferedImageCacheEntry extends CacheEntry {
    private static final long serialVersionUID = 1L; //version
    // transient to avoid serialization, volatile to avoid synchronization of whole getImage() method
    private transient volatile BufferedImage img = null;
    private transient volatile boolean writtenToDisk = false;

    /**
     *
     * @param content byte array containing image
     */
    public BufferedImageCacheEntry(byte[] content) {
        super(content);
    }

    /**
     * Returns BufferedImage from for the content. Subsequent calls will return the same instance,
     * to reduce overhead of ImageIO
     *
     * @return BufferedImage of cache entry content
     * @throws IOException
     */
    public BufferedImage getImage() throws IOException {
        if (img != null)
            return img;
        synchronized(this) {
            if (img != null)
                return img;
            byte[] content = getContent();
            if (content != null && content.length > 0) {
                img = ImageIO.read(new ByteArrayInputStream(content));

                if (writtenToDisk)
                    content = null;
            }

        }
        return img;
    }


    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        /*
         * This method below will be needed, if Apache Commons JCS (or any other caching system), will update
         * disk representation of object from memory, once it is put into the cache (for example - at closing the cache)
         *
         * For now it is not the case, as we use DiskUsagePattern.UPDATE, which on JCS shutdown doesn't write again memory
         * contents to file, so the fact, that we've cleared never gets saved to the disk
         *
         * This method is commented out, as it will convert all cache entries to PNG files regardless of what was returned.
         * It might cause recompression/change of format which may result in decreased quality of imagery
         */
        /* synchronized (this) {
            if (content == null && img != null) {
                ByteArrayOutputStream restoredData = new ByteArrayOutputStream();
                ImageIO.write(img, "png", restoredData);
                content = restoredData.toByteArray();
            }
            out.writeObject(this);
        }
         */
        synchronized (this) {
            if (content == null && img != null) {
                throw new AssertionError("Trying to serialize (save to disk?) an BufferedImageCacheEntry that was converted to BufferedImage and no raw data is present anymore");
            }
            out.writeObject(this);
            // ugly hack to wait till element will get to disk to clean the memory
            writtenToDisk = true;

            if (img != null) {
                content = null;
            }

        }
    }
}
