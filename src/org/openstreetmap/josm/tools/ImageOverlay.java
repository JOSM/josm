// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

/** class to describe how image overlay
 * @since 8095
 */
public class ImageOverlay implements ImageProcessor {
    /** the image resource to use as overlay */
    public ImageProvider image;
    /** offset of the image from left border, values between 0 and 1 */
    private final double offsetLeft;
    /** offset of the image from top border, values between 0 and 1 */
    private final double offsetRight;
    /** offset of the image from right border, values between 0 and 1*/
    private final double offsetTop;
    /** offset of the image from bottom border, values between 0 and 1 */
    private final double offsetBottom;

    /**
     * Create an overlay info. All values are relative sizes between 0 and 1. Size of the image
     * is the result of the difference between left/right and top/bottom.
     *
     * @param image image provider for the overlay icon
     * @param offsetLeft offset of the image from left border, values between 0 and 1, -1 for auto-calculation
     * @param offsetTop offset of the image from top border, values between 0 and 1, -1 for auto-calculation
     * @param offsetRight offset of the image from right border, values between 0 and 1, -1 for auto-calculation
     * @param offsetBottom offset of the image from bottom border, values between 0 and 1, -1 for auto-calculation
     * @since 8095
     */
    public ImageOverlay(ImageProvider image, double offsetLeft, double offsetTop, double offsetRight, double offsetBottom) {
        this.image = image;
        this.offsetLeft = offsetLeft;
        this.offsetTop = offsetTop;
        this.offsetRight = offsetRight;
        this.offsetBottom = offsetBottom;
    }

    /**
     * Create an overlay in southeast corner. All values are relative sizes between 0 and 1.
     * Size of the image is the result of the difference between left/right and top/bottom.
     * Right and bottom values are set to 1.
     *
     * @param image image provider for the overlay icon
     * @see #ImageOverlay(ImageProvider, double, double, double, double)
     * @since 8095
     */
    public ImageOverlay(ImageProvider image) {
        this.image = image;
        this.offsetLeft = -1.0;
        this.offsetTop = -1.0;
        this.offsetRight = 1.0;
        this.offsetBottom = 1.0;
    }

    /**
     * Handle overlay. The image passed as argument is modified!
     *
     * @param ground the base image for the overlay (gets modified!)
     * @return the modified image (same as argument)
     * @since 8095
     */
    @Override
    public BufferedImage process(BufferedImage ground) {
        /* get base dimensions for calculation */
        int w = ground.getWidth();
        int h = ground.getHeight();
        int width = -1;
        int height = -1;
        if (offsetRight > 0 && offsetLeft > 0) {
            width = (int) (w*(offsetRight-offsetLeft));
        }
        if (offsetTop > 0 && offsetBottom > 0) {
            height = (int) (h*(offsetBottom-offsetTop));
        }
        ImageIcon overlay;
        image = new ImageProvider(image).setMaxSize(new Dimension(width, height));
        overlay = image.get();
        int x, y;
        if (width == -1 && offsetLeft < 0) {
            x = (int) (w*offsetRight) - overlay.getIconWidth();
        } else {
            x = (int) (w*offsetLeft);
        }
        if (height == -1 && offsetTop < 0) {
            y = (int) (h*offsetBottom) - overlay.getIconHeight();
        } else {
            y = (int) (h*offsetTop);
        }
        overlay.paintIcon(null, ground.getGraphics(), x, y);
        return ground;
    }
}
