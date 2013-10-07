// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.kitfox.svg.SVGDiagram;

/**
 * Holds data for one particular image.
 * It can be backed by a svg or raster image.
 *
 * In the first case, 'svg' is not null and in the latter case, 'imgCache' has
 * at least one entry for the key DEFAULT_DIMENSION.
 * @since 4271
 */
class ImageResource {

    /**
     * Caches the image data for resized versions of the same image.
     */
    private Map<Dimension, Image> imgCache = new HashMap<Dimension, Image>();
    private SVGDiagram svg;
    public static final Dimension DEFAULT_DIMENSION = new Dimension(-1, -1);

    public ImageResource(Image img) {
        CheckParameterUtil.ensureParameterNotNull(img);
        imgCache.put(DEFAULT_DIMENSION, img);
    }

    public ImageResource(SVGDiagram svg) {
        CheckParameterUtil.ensureParameterNotNull(svg);
        this.svg = svg;
    }

    public ImageIcon getImageIcon() {
        return getImageIcon(DEFAULT_DIMENSION);
    }

    /**
     * Get an ImageIcon object for the image of this resource
     * @param   dim The requested dimensions. Use (-1,-1) for the original size
     *          and (width, -1) to set the width, but otherwise scale the image
     *          proportionally.
     * @return ImageIcon object for the image of this resource, scaled according to dim
     */
    public ImageIcon getImageIcon(Dimension dim) {
        if (dim.width < -1 || dim.width == 0 || dim.height < -1 || dim.height == 0)
            throw new IllegalArgumentException();
        Image img = imgCache.get(dim);
        if (img != null) {
            return new ImageIcon(img);
        }
        if (svg != null) {
            img = ImageProvider.createImageFromSvg(svg, dim);
            if (img == null) {
                return null;
            }
            imgCache.put(dim, img);
            return new ImageIcon(img);
        } else {
            Image base = imgCache.get(DEFAULT_DIMENSION);
            if (base == null) throw new AssertionError();

            int width = dim.width;
            int height = dim.height;
            ImageIcon icon = new ImageIcon(base);
            if (width == -1) {
                width = Math.max(1, icon.getIconWidth() * height / icon.getIconHeight());
            } else if (height == -1) {
                height = Math.max(1, icon.getIconHeight() * width / icon.getIconWidth());
            }
            Image i = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            img.getGraphics().drawImage(i, 0, 0, null);
            imgCache.put(dim, img);
            return new ImageIcon(img);
        }
    }

    /**
     * Get image icon with a certain maximum size. The image is scaled down
     * to fit maximum dimensions. (Keeps aspect ratio)
     *
     * @param maxSize The maximum size. One of the dimensions (widht or height) can be -1,
     * which means it is not bounded.
     * @return ImageIcon object for the image of this resource, scaled down if needed, according to maxSize
     */
    public ImageIcon getImageIconBounded(Dimension maxSize) {
        if (maxSize.width < -1 || maxSize.width == 0 || maxSize.height < -1 || maxSize.height == 0)
            throw new IllegalArgumentException();
        float realWidth;
        float realHeight;
        if (svg != null) {
            realWidth = svg.getWidth();
            realHeight = svg.getHeight();
        } else {
            Image base = imgCache.get(DEFAULT_DIMENSION);
            if (base == null) throw new AssertionError();
            ImageIcon icon = new ImageIcon(base);
            realWidth = icon.getIconWidth();
            realHeight = icon.getIconHeight();
        }
        int maxWidth = maxSize.width;
        int maxHeight = maxSize.height;

        if (realWidth <= maxWidth) {
            maxWidth = -1;
        }
        if (realHeight <= maxHeight) {
            maxHeight = -1;
        }

        if (maxWidth == -1 && maxHeight == -1)
            return getImageIcon(DEFAULT_DIMENSION);
        else if (maxWidth == -1)
            return getImageIcon(new Dimension(-1, maxHeight));
        else if (maxHeight == -1)
            return getImageIcon(new Dimension(maxWidth, -1));
        else
            if (realWidth / maxWidth > realHeight / maxHeight)
                return getImageIcon(new Dimension(maxWidth, -1));
            else
                return getImageIcon(new Dimension(-1, maxHeight));
   }
}
