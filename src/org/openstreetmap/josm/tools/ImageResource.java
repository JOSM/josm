// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;

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
     * Caches the image data for resized versions of the same image. The key is obtained using {@link ImageResizeMode#cacheKey(Dimension)}.
     */
    private final Map<Integer, BufferedImage> imgCache = new ConcurrentHashMap<>(4);
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
    /**
     * <code>true</code> if icon must be grayed out
     */
    protected boolean isDisabled;
    /**
     * The base raster image for the final output
     */
    private Image baseImage;

    /**
     * Constructs a new {@code ImageResource} from an image.
     * @param img the image
     */
    public ImageResource(Image img) {
        CheckParameterUtil.ensureParameterNotNull(img);
        baseImage = img;
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
     * Set, if image must be filtered to grayscale so it will look like disabled icon.
     *
     * @param disabled true, if image must be grayed out for disabled state
     * @return the current object, for convenience
     * @since 10428
     */
    public ImageResource setDisabled(boolean disabled) {
        this.isDisabled = disabled;
        return this;
    }

    /**
     * Set both icons of an Action
     * @param a The action for the icons
     * @since 10369
     */
    public void attachImageIcon(AbstractAction a) {
        Dimension iconDimension = ImageProvider.ImageSizes.SMALLICON.getImageDimension();
        ImageIcon icon = getImageIconBounded(iconDimension);
        a.putValue(Action.SMALL_ICON, icon);

        iconDimension = ImageProvider.ImageSizes.LARGEICON.getImageDimension();
        icon = getImageIconBounded(iconDimension);
        a.putValue(Action.LARGE_ICON_KEY, icon);
    }

    /**
     * Set both icons of an Action
     * @param a The action for the icons
     * @param attachImageResource Adds an resource named "ImageResource" if <code>true</code>
     * @since 10369
     */
    public void attachImageIcon(AbstractAction a, boolean attachImageResource) {
        attachImageIcon(a);
        if (attachImageResource) {
            a.putValue("ImageResource", this);
        }
    }

    /**
     * Returns the image icon at default dimension.
     * @return the image icon at default dimension
     */
    public ImageIcon getImageIcon() {
        return getImageIcon(DEFAULT_DIMENSION);
    }

    /**
     * Get an ImageIcon object for the image of this resource.
     * <p>
     * Will return a multi-resolution image by default (if possible).
     * @param  dim The requested dimensions. Use (-1,-1) for the original size and (width, -1)
     *         to set the width, but otherwise scale the image proportionally.
     * @return ImageIcon object for the image of this resource, scaled according to dim
     * @see #getImageIconBounded(java.awt.Dimension)
     */
    public ImageIcon getImageIcon(Dimension dim) {
        return getImageIcon(dim, true, ImageResizeMode.AUTO);
    }

    /**
     * Get an ImageIcon object for the image of this resource.
     * @param  dim The requested dimensions. Use (-1,-1) for the original size and (width, -1)
     *         to set the width, but otherwise scale the image proportionally.
     * @param  multiResolution If true, return a multi-resolution image
     * (java.awt.image.MultiResolutionImage in Java 9), otherwise a plain {@link BufferedImage}.
     * When running Java 8, this flag has no effect and a plain image will be returned in any case.
     * @param resizeMode how to size/resize the image
     * @return ImageIcon object for the image of this resource, scaled according to dim
     * @since 12722
     */
    ImageIcon getImageIcon(Dimension dim, boolean multiResolution, ImageResizeMode resizeMode) {
        return getImageIconAlreadyScaled(GuiSizesHelper.getDimensionDpiAdjusted(dim), multiResolution, false, resizeMode);
    }

    /**
     * Get an ImageIcon object for the image of this resource. A potential UI scaling is assumed
     * to be already taken care of, so dim is already scaled accordingly.
     * @param  dim The requested dimensions. Use (-1,-1) for the original size and (width, -1)
     *         to set the width, but otherwise scale the image proportionally.
     * @param  multiResolution If true, return a multi-resolution image
     * (java.awt.image.MultiResolutionImage in Java 9), otherwise a plain {@link BufferedImage}.
     * When running Java 8, this flag has no effect and a plain image will be returned in any case.
     * @param highResolution whether the high resolution variant should be used for overlays
     * @param resizeMode how to size/resize the image
     * @return ImageIcon object for the image of this resource, scaled according to dim
     */
    ImageIcon getImageIconAlreadyScaled(Dimension dim, boolean multiResolution, boolean highResolution, ImageResizeMode resizeMode) {
        CheckParameterUtil.ensureThat((dim.width > 0 || dim.width == -1) && (dim.height > 0 || dim.height == -1),
                () -> dim + " is invalid");

        final int cacheKey = resizeMode.cacheKey(dim);
        BufferedImage img = imgCache.get(cacheKey);
        if (img == null) {
            if (svg != null) {
                img = ImageProvider.createImageFromSvg(svg, dim, resizeMode);
                if (img == null) {
                    return null;
                }
            } else {
                if (baseImage == null) throw new AssertionError();
                ImageIcon icon = new ImageIcon(baseImage);
                img = resizeMode.createBufferedImage(dim, new Dimension(icon.getIconWidth(), icon.getIconHeight()),
                        null, icon.getImage());
            }
            if (overlayInfo != null) {
                for (ImageOverlay o : overlayInfo) {
                    o.process(img, highResolution);
                }
            }
            if (isDisabled) {
                //Use default Swing functionality to make icon look disabled by applying grayscaling filter.
                Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(null, new ImageIcon(img));
                if (disabledIcon == null) {
                    return null;
                }

                //Convert Icon to ImageIcon with BufferedImage inside
                img = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                disabledIcon.paintIcon(new JPanel(), img.getGraphics(), 0, 0);
            }
            imgCache.put(cacheKey, img);
        }

        if (!multiResolution)
            return new ImageIcon(img);
        else {
            try {
                Image mrImg = HiDPISupport.getMultiResolutionImage(img, this, resizeMode);
                return new ImageIcon(mrImg);
            } catch (NoClassDefFoundError e) {
                Logging.trace(e);
                return new ImageIcon(img);
            }
        }
    }

    /**
     * Get image icon with a certain maximum size. The image is scaled down
     * to fit maximum dimensions. (Keeps aspect ratio)
     * <p>
     * Will return a multi-resolution image by default (if possible).
     *
     * @param maxSize The maximum size. One of the dimensions (width or height) can be -1,
     * which means it is not bounded.
     * @return ImageIcon object for the image of this resource, scaled down if needed, according to maxSize
     */
    public ImageIcon getImageIconBounded(Dimension maxSize) {
        return getImageIcon(maxSize, true, ImageResizeMode.BOUNDED);
    }

    /**
     * Returns an {@link ImageIcon} for the given map image, at the specified size.
     * Uses a cache to improve performance.
     * @param iconSize size in pixels
     * @return an {@code ImageIcon} for the given map image, at the specified size
     */
    public ImageIcon getPaddedIcon(Dimension iconSize) {
        return getImageIcon(iconSize, true, ImageResizeMode.PADDED);
    }

    @Override
    public String toString() {
        return "ImageResource ["
                + (svg != null ? "svg=" + svg : "")
                + (baseImage != null ? "baseImage=" + baseImage : "") + ']';
    }
}
