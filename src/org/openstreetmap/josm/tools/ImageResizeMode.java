// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Determines how the image is sized/resized in {@link ImageResource#getImageIcon(Dimension, boolean, ImageResizeMode)}.
 */
enum ImageResizeMode {

    AUTO {
        @Override
        Dimension computeDimension(Dimension dim, Dimension icon) {
            CheckParameterUtil.ensureThat((dim.width > 0 || dim.width == -1) && (dim.height > 0 || dim.height == -1),
                    () -> dim + " is invalid");
            if (dim.width == -1 && dim.height == -1) {
                return new Dimension(GuiSizesHelper.getSizeDpiAdjusted(icon.width), GuiSizesHelper.getSizeDpiAdjusted(icon.height));
            } else if (dim.width == -1) {
                return new Dimension(Math.max(1, icon.width * dim.height / icon.height), dim.height);
            } else if (dim.height == -1) {
                return new Dimension(dim.width, Math.max(1, icon.height * dim.width / icon.width));
            } else {
                return dim;
            }
        }
    },

    BOUNDED {
        @Override
        Dimension computeDimension(Dimension dim, Dimension icon) {
            final int maxWidth = Math.min(dim.width, icon.width);
            final int maxHeight = Math.min(dim.height, icon.height);
            return BOUNDED_UPSCALE.computeDimension(new Dimension(maxWidth, maxHeight), icon);
        }
    },

    BOUNDED_UPSCALE {
        @Override
        Dimension computeDimension(Dimension dim, Dimension icon) {
            CheckParameterUtil.ensureThat((dim.width > 0 || dim.width == -1) && (dim.height > 0 || dim.height == -1),
                    () -> dim + " is invalid");
            final int maxWidth = dim.width;
            final int maxHeight = dim.height;
            final Dimension spec;
            if (maxWidth == -1 || maxHeight == -1) {
                spec = dim;
            } else if (icon.getWidth() / maxWidth > icon.getHeight() / maxHeight) {
                spec = new Dimension(maxWidth, -1);
            } else {
                spec = new Dimension(-1, maxHeight);
            }
            return AUTO.computeDimension(spec, icon);
        }
    },

    PADDED {
        @Override
        Dimension computeDimension(Dimension dim, Dimension icon) {
            CheckParameterUtil.ensureThat(dim.width > 0 && dim.height > 0, () -> dim + " is invalid");
            return dim;
        }

        @Override
        void prepareGraphics(Dimension icon, BufferedImage image, Graphics2D g) {
            g.setClip(0, 0, image.getWidth(), image.getHeight());
            final double scale = Math.min(image.getWidth() / icon.getWidth(), image.getHeight() / icon.getHeight());
            g.translate((image.getWidth() - icon.getWidth() * scale) / 2, (image.getHeight() - icon.getHeight() * scale) / 2);
            g.scale(scale, scale);
        }
    };

    /**
     * Computes the dimension for the resulting image
     * @param dim the desired image dimension
     * @param icon the dimensions of the image to resize
     * @return the dimension for the resulting image
     */
    abstract Dimension computeDimension(Dimension dim, Dimension icon);

    /**
     * Creates a new buffered image and applies the rendering function
     * @param dim the desired image dimension
     * @param icon the dimensions of the image to resize
     * @param renderer the rendering function
     * @param sourceIcon the source icon to draw
     * @return a new buffered image
     * @throws IllegalArgumentException if renderer or sourceIcon is null
     */
    BufferedImage createBufferedImage(Dimension dim, Dimension icon, Consumer<Graphics2D> renderer, Image sourceIcon) {
        final Dimension real = computeDimension(dim, icon);
        final BufferedImage bufferedImage = new BufferedImage(real.width, real.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = bufferedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (renderer != null) {
            prepareGraphics(icon, bufferedImage, g);
            renderer.accept(g);
        } else if (sourceIcon != null) {
            sourceIcon = sourceIcon.getScaledInstance(real.width, real.height, Image.SCALE_SMOOTH);
            g.drawImage(sourceIcon, 0, 0, null);
        } else {
            throw new IllegalArgumentException("renderer or sourceIcon");
        }
        return bufferedImage;
    }

    /**
     * Prepares the graphics object for rendering the given image
     * @param icon the dimensions of the image to resize
     * @param image the image to render afterwards
     * @param g graphics
     */
    void prepareGraphics(Dimension icon, BufferedImage image, Graphics2D g) {
        g.setClip(0, 0, image.getWidth(), image.getHeight());
        g.scale(image.getWidth() / icon.getWidth(), image.getHeight() / icon.getHeight());
    }

    /**
     * Returns a cache key for this mode and the given dimension
     * @param dim the desired image dimension
     * @return a cache key
     */
    int cacheKey(Dimension dim) {
        return (ordinal() << 28) | ((dim.width & 0xfff) << 16) | (dim.height & 0xfff);
    }

}
