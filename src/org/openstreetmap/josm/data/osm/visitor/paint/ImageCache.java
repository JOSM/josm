// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Image;

import jakarta.annotation.Nullable;

/**
 * A record for keeping the image information for a tile. Used in conjunction with {@link TileZXY} for
 * {@link org.openstreetmap.josm.data.cache.JCSCacheManager}.
 * @since 19176
 */
public final class ImageCache {
    private final boolean isDirty;
    private final StyledTiledMapRenderer.TileLoader imageFuture;
    private final Image image;
    /**
     * Create a new {@link ImageCache} object
     * @param image The image to paint (optional; either this or {@link #imageFuture} must be specified)
     * @param imageFuture The future for the image (optional; either this or {@link #image} must be specified)
     * @param isDirty {@code true} if the tile needs to be repainted
     */
    ImageCache(Image image, StyledTiledMapRenderer.TileLoader imageFuture, boolean isDirty) {
        this.image = image;
        this.imageFuture = imageFuture;
        this.isDirty = isDirty;
        if (image == null && imageFuture == null) {
            throw new IllegalArgumentException("Either image or imageFuture must be non-null");
        }
    }

    /**
     * Check if this tile is dirty
     * @return {@code true} if this is a dirty tile
     */
    public boolean isDirty() {
        return this.isDirty;
    }

    /**
     * Mark this tile as dirty
     * @return The tile to put in the cache
     */
    public ImageCache becomeDirty() {
        if (this.isDirty) {
            return this;
        }
        return new ImageCache(this.image, this.imageFuture, true);
    }

    /**
     * Get the image to paint
     * @return The image (may be {@code null})
     */
    @Nullable
    public Image image() {
        return this.image;
    }

    /**
     * Get the image future
     * @return The image future (may be {@code null})
     */
    @Nullable
    StyledTiledMapRenderer.TileLoader imageFuture() {
        return this.imageFuture;
    }
}
