// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.apache.commons.jcs3.engine.behavior.ICache;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Stopwatch;

/**
 * Loads thumbnail previews for a list of images from a {@link GeoImageLayer}.
 *
 * Thumbnails are loaded in the background and cached on disk for the next session.
 */
public class ThumbsLoader implements Runnable {
    public static final int maxSize = 120;
    public static final int minSize = 22;
    public volatile boolean stop;
    private final Collection<ImageEntry> data;
    private final GeoImageLayer layer;
    private ICacheAccess<String, BufferedImageCacheEntry> cache;
    private final boolean cacheOff = Config.getPref().getBoolean("geoimage.noThumbnailCache", false);

    private ThumbsLoader(Collection<ImageEntry> data, GeoImageLayer layer) {
        this.data = data;
        this.layer = layer;
        initCache();
    }

    /**
     * Constructs a new thumbnail loader that operates on a geoimage layer.
     * @param layer geoimage layer
     */
    public ThumbsLoader(GeoImageLayer layer) {
        this(new ArrayList<>(layer.getImageData().getImages()), layer);
    }

    /**
     * Constructs a new thumbnail loader that operates on the image entries
     * @param entries image entries
     */
    public ThumbsLoader(Collection<ImageEntry> entries) {
        this(entries, null);
    }

    /**
     * Initialize the thumbnail cache.
     */
    private void initCache() {
        if (!cacheOff) {
            cache = JCSCacheManager.getCache("geoimage-thumbnails", 0, 120,
                    Config.getDirs().getCacheDirectory(true).getPath() + File.separator + "geoimage-thumbnails");
        }
    }

    @Override
    public void run() {
        int count = 0;
        Stopwatch stopwatch = Stopwatch.createStarted();
        Logging.debug("Loading {0} thumbnails", data.size());
        for (ImageEntry entry : data) {
            if (stop) return;

            // Do not load thumbnails that were loaded before.
            if (!entry.hasThumbnail()) {
                entry.setThumbnail(loadThumb(entry));

                if (layer != null && MainApplication.isDisplayingMapView()) {
                    layer.updateBufferAndRepaint();
                }
            }
            count++;
        }
        Logging.debug("Loaded {0} thumbnails in {1}", count, stopwatch);
        if (layer != null) {
            layer.thumbsLoaded();
            layer.updateBufferAndRepaint();
        }
    }

    private BufferedImage loadThumb(ImageEntry entry) {
        final String cacheIdent = entry.getFile().toString() + ICache.NAME_COMPONENT_DELIMITER + maxSize;

        if (!cacheOff && cache != null) {
            try {
                BufferedImageCacheEntry cacheEntry = cache.get(cacheIdent);
                if (cacheEntry != null && cacheEntry.getImage() != null) {
                    Logging.debug("{0} from cache", cacheIdent);
                    return cacheEntry.getImage();
                }
            } catch (IOException e) {
                Logging.warn(e);
            }
        }

        BufferedImage img;
        try {
            img = entry.read(new Dimension(maxSize, maxSize));
        } catch (IOException e) {
            Logging.warn("Failed to load geoimage thumb");
            Logging.warn(e);
            return null;
        }

        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
            Logging.error(" Invalid image");
            return null;
        }

        if (!cacheOff && cache != null) {
            try {
                cache.put(cacheIdent, BufferedImageCacheEntry.pngEncoded(img));
            } catch (UncheckedIOException e) {
                Logging.warn("Failed to save geoimage thumb to cache");
                Logging.warn(e);
            }
        }

        return img;
    }
}
