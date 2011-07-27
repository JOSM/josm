// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import com.kitfox.svg.SVGDiagram;

import java.awt.Dimension;
import java.awt.Image;
import java.util.HashMap;
import javax.swing.ImageIcon;

/**
 * Holds data for one particular image.
 * It can be backed by a svg or raster image.
 * 
 * In the first case, 'svg' is not null and in the latter case, 'imgCache' has 
 * at least one entry for the key DEFAULT_DIMENSION.
 */
class ImageResource {
    
    /**
     * Caches the image data for resized versions of the same image.
     */
    private HashMap<Dimension, ImageWrapper> imgCache = new HashMap<Dimension, ImageWrapper>();
    private SVGDiagram svg;
    public static final Dimension DEFAULT_DIMENSION = new Dimension(-1, -1);
 
    /**
     * remember whether the image has been sanitized
     */
    private static class ImageWrapper {
        Image img;
        boolean sanitized;

        public ImageWrapper(Image img, boolean sanitized) {
            CheckParameterUtil.ensureParameterNotNull(img);
            this.img = img;
            this.sanitized = sanitized;
        }
    }
    
    public ImageResource(Image img, boolean sanitized) {
        CheckParameterUtil.ensureParameterNotNull(img);
        imgCache.put(DEFAULT_DIMENSION, new ImageWrapper(img, sanitized));
    }

    public ImageResource(SVGDiagram svg) {
        CheckParameterUtil.ensureParameterNotNull(svg);
        this.svg = svg;
    }

    public ImageIcon getImageIcon() {
        return getImageIcon(DEFAULT_DIMENSION, false);
    }

    /**
     * Get an ImageIcon object for the image of this resource
     * @param   dim The requested dimensions. Use (-1,-1) for the original size
     *          and (width, -1) to set the width, but otherwise scale the image
     *          proportionally.
     * @param sanitized Whether the returned image should be copied to a BufferedImage
     *          to avoid certain problem with native image formats.
     */
    public ImageIcon getImageIcon(Dimension dim, boolean sanitized) {
        ImageWrapper iw = imgCache.get(dim);
        if (iw != null) {
            if (sanitized && !iw.sanitized) {
                iw.img = ImageProvider.sanitize(iw.img);
                iw.sanitized = true;
            }
            return new ImageIcon(iw.img);
        }
        if (svg != null) {
            Image img = ImageProvider.createImageFromSvg(svg, dim);
            imgCache.put(dim, new ImageWrapper(img, true));
            return new ImageIcon(img);
        } else {
            ImageWrapper base = imgCache.get(DEFAULT_DIMENSION);
            if (base == null) throw new AssertionError();
            
            int width = dim.width;
            int height = dim.height;
            ImageIcon icon = new ImageIcon(base.img);
            if (width == -1) {
                width = icon.getIconWidth() * height / icon.getIconHeight();
            } else if (height == -1) {
                height = icon.getIconHeight() * width / icon.getIconWidth();
            }
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            if (sanitized) {
                img = ImageProvider.sanitize(img);
            }
            imgCache.put(dim, new ImageWrapper(img, sanitized));
            return new ImageIcon(img);
        }
    }
}
