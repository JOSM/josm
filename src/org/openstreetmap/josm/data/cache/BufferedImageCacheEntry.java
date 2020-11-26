// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/**
 * Cache Entry that has methods to get the BufferedImage, that will be cached along in memory
 * but will be not serialized when saved to the disk (to avoid duplication of data)
 *
 * @author Wiktor Niesiobędzki
 */
public class BufferedImageCacheEntry extends CacheEntry {
    private static final long serialVersionUID = 1L; //version
    // transient to avoid serialization, volatile to avoid synchronization of whole getImage() method
    private transient volatile BufferedImage img;
    // we need to have separate control variable, to know, if we already tried to load the image, as img might be null
    // after we loaded image, as for example, when image file is malformed (eg. HTML file)
    private transient volatile boolean imageLoaded;

    /**
     *
     * @param content byte array containing image
     */
    public BufferedImageCacheEntry(byte[] content) {
        super(content);
    }

    /**
     * Encodes the given image as PNG and returns a cache entry
     * @param img the image
     * @return a cache entry for the PNG encoded image
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static BufferedImageCacheEntry pngEncoded(BufferedImage img) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", output);
            return new BufferedImageCacheEntry(output.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns BufferedImage from for the content. Subsequent calls will return the same instance,
     * to reduce overhead of ImageIO
     *
     * @return BufferedImage of cache entry content
     * @throws IOException if an error occurs during reading.
     */
    public BufferedImage getImage() throws IOException {
        if (imageLoaded)
            return img;
        synchronized (this) {
            if (imageLoaded)
                return img;
            byte[] content = getContent();
            if (content.length > 0) {
                img = ImageIO.read(new ByteArrayInputStream(content));
                imageLoaded = true;
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
                throw new AssertionError("Trying to serialize (save to disk?) an BufferedImageCacheEntry " +
                        "that was converted to BufferedImage and no raw data is present anymore");
            }
            out.writeObject(this);

            if (img != null) {
                content = null;
            }
        }
    }
}
