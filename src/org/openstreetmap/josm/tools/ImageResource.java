// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import com.kitfox.svg.SVGDiagram;

/**
 * Holds data for one particular image.
 * It can be backed by a svg or raster image.
 *
 * In the first case, <code>svg</code> is not <code>null</code> and in the latter case,
 * <code>baseImage</code> is not <code>null</code>.
 * @since 4271
 */
public class ImageResource {

    /**
     * Caches the image data for resized versions of the same image.
     */
    private Map<Dimension, Image> imgCache = new HashMap<>();
    /**
     * SVG diagram information in case of SVG vector image.
     */
    private SVGDiagram svg;
    /**
     * Use this dimension to request original file dimension.
     */
    public static final Dimension DEFAULT_DIMENSION = new Dimension(-1, -1);
    /**
     * ordered list of overlay images
     */
    protected List<ImageOverlay> overlayInfo;
    private Image baseImage;

    /**
     * Constructs a new {@code ImageResource} from an image.
     * @param img the image
     */
    public ImageResource(Image img) {
        CheckParameterUtil.ensureParameterNotNull(img);
        this.baseImage = img;
        imgCache.put(DEFAULT_DIMENSION, img);
    }

    /**
     * Constructs a new {@code ImageResource} from SVG data.
     * @param svg SVG data
     */
    public ImageResource(SVGDiagram svg) {
        CheckParameterUtil.ensureParameterNotNull(svg);
        this.svg = svg;
    }

    /**
     * Constructs a new {@code ImageResource} from another one and sets overlays.
     * @param res the existing resource
     * @param overlayInfo the overlay to apply
     * @since 8095
     */
    public ImageResource(ImageResource res, List<ImageOverlay> overlayInfo) {
        this.svg = res.svg;
        this.baseImage = res.baseImage;
        this.overlayInfo = overlayInfo;
    }

    /**
     * Returns the image icon at default dimension.
     * @return the image icon at default dimension
     */
    public ImageIcon getImageIcon() {
        return getImageIcon(DEFAULT_DIMENSION);
    }

    /**
     * Set both icons of an Action
     * @param a The action for the icons
     * @since 7693
     */
    public void getImageIcon(AbstractAction a) {
        ImageIcon icon = getImageIconBounded(ImageProvider.getImageSizes(ImageProvider.ImageSizes.SMALLICON));
        a.putValue(Action.SMALL_ICON, icon);
        icon = getImageIconBounded(ImageProvider.getImageSizes(ImageProvider.ImageSizes.LARGEICON));
        a.putValue(Action.LARGE_ICON_KEY, icon);
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
            throw new IllegalArgumentException(dim+" is invalid");
        Image img = imgCache.get(dim);
        if (img != null) {
            return new ImageIcon(img);
        }
        if (svg != null) {
            BufferedImage bimg = ImageProvider.createImageFromSvg(svg, dim);
            if (bimg == null) {
                return null;
            }
            if (overlayInfo != null) {
                for (ImageOverlay o : overlayInfo) {
                    o.process(bimg);
                }
            }
            imgCache.put(dim, bimg);
            return new ImageIcon(bimg);
        } else {
            if (baseImage == null) throw new AssertionError();

            int width = dim.width;
            int height = dim.height;
            ImageIcon icon = new ImageIcon(baseImage);
            if (width == -1 && height == -1) {
                width = icon.getIconWidth();
                height = icon.getIconHeight();
            } else if (width == -1) {
                width = Math.max(1, icon.getIconWidth() * height / icon.getIconHeight());
            } else if (height == -1) {
                height = Math.max(1, icon.getIconHeight() * width / icon.getIconWidth());
            }
            Image i = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bimg.getGraphics().drawImage(i, 0, 0, null);
            if (overlayInfo != null) {
                for (ImageOverlay o : overlayInfo) {
                    o.process(bimg);
                }
            }
            imgCache.put(dim, bimg);
            return new ImageIcon(bimg);
        }
    }

    /**
     * Get image icon with a certain maximum size. The image is scaled down
     * to fit maximum dimensions. (Keeps aspect ratio)
     *
     * @param maxSize The maximum size. One of the dimensions (width or height) can be -1,
     * which means it is not bounded.
     * @return ImageIcon object for the image of this resource, scaled down if needed, according to maxSize
     */
    public ImageIcon getImageIconBounded(Dimension maxSize) {
        if (maxSize.width < -1 || maxSize.width == 0 || maxSize.height < -1 || maxSize.height == 0)
            throw new IllegalArgumentException(maxSize+" is invalid");
        float realWidth;
        float realHeight;
        if (svg != null) {
            realWidth = svg.getWidth();
            realHeight = svg.getHeight();
        } else {
            if (baseImage == null) throw new AssertionError();
            ImageIcon icon = new ImageIcon(baseImage);
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
        else if (realWidth / maxWidth > realHeight / maxHeight)
            return getImageIcon(new Dimension(maxWidth, -1));
        else
            return getImageIcon(new Dimension(-1, maxHeight));
   }
}
