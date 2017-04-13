// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Image warping algorithm.
 *
 * Deforms an image geometrically according to a given transformation formula.
 * @since 11858
 */
public class ImageWarp {

    /**
     * Transformation that translates the pixel coordinates.
     */
    public interface PointTransform {
        Point2D transform(Point2D pt);
    }

    /**
     * Interpolation method.
     */
    public enum Interpolation {
        /**
         * Nearest neighbor.
         *
         * Simplest possible method. Faster, but not very good quality.
         */
        NEAREST_NEIGHBOR(1),
        /**
         * Bilinear.
         *
         * Decent quality.
         */
        BILINEAR(2);

        private final int margin;

        private Interpolation(int margin) {
            this.margin = margin;
        }

        /**
         * Number of pixels to scan outside the source image.
         * Used to get smoother borders.
         * @return the margin
         */
        public int getMargin() {
            return margin;
        }
    }

    /**
     * Warp an image.
     * @param srcImg the original image
     * @param targetDim dimension of the target image
     * @param invTransform inverse transformation (translates pixel coordinates
     * of the target image to pixel coordinates of the original image)
     * @param interpolation the interpolation method
     * @return the warped image
     */
    public static BufferedImage warp(BufferedImage srcImg, Dimension targetDim, PointTransform invTransform, Interpolation interpolation) {
        BufferedImage imgTarget = new BufferedImage(targetDim.width, targetDim.height, BufferedImage.TYPE_INT_ARGB);
        Rectangle2D srcRect = new Rectangle2D.Double(0, 0, srcImg.getWidth(), srcImg.getHeight());
        for (int j = 0; j < imgTarget.getHeight(); j++) {
            for (int i = 0; i < imgTarget.getWidth(); i++) {
                Point2D srcCoord = invTransform.transform(new Point2D.Double(i, j));
                if (isInside(srcCoord, srcRect, interpolation.getMargin())) {
                    int rgba;
                    switch (interpolation) {
                        case NEAREST_NEIGHBOR:
                            rgba = getColor((int) Math.round(srcCoord.getX()), (int) Math.round(srcCoord.getY()), srcImg);
                            break;
                        case BILINEAR:
                            int x0 = (int) Math.floor(srcCoord.getX());
                            double dx = srcCoord.getX() - x0;
                            int y0 = (int) Math.floor(srcCoord.getY());
                            double dy = srcCoord.getY() - y0;
                            int c00 = getColor(x0, y0, srcImg);
                            int c01 = getColor(x0, y0 + 1, srcImg);
                            int c10 = getColor(x0 + 1, y0, srcImg);
                            int c11 = getColor(x0 + 1, y0 + 1, srcImg);
                            rgba = 0;
                            // loop over color components: blue, green, red, alpha
                            for (int ch = 0; ch <= 3; ch++) {
                                int shift = 8 * ch;
                                int chVal = (int) Math.round(
                                    (((c00 >> shift) & 0xff) * (1-dx) + ((c10 >> shift) & 0xff) * dx) * (1-dy) +
                                    (((c01 >> shift) & 0xff) * (1-dx) + ((c11 >> shift) & 0xff) * dx) * dy);
                                rgba |= chVal << shift;
                            }
                            break;
                        default:
                            throw new AssertionError();
                    }
                    imgTarget.setRGB(i, j, rgba);
                }
            }
        }
        return imgTarget;
    }

    private static boolean isInside(Point2D p, Rectangle2D rect, double margin) {
        return isInside(p.getX(), rect.getMinX(), rect.getMaxX(), margin) &&
                isInside(p.getY(), rect.getMinY(), rect.getMaxY(), margin);
    }

    private static boolean isInside(double x, double xMin, double xMax, double margin) {
        return x + margin >= xMin && x - margin <= xMax;
    }

    private static int getColor(int x, int y, BufferedImage img) {
        // border strategy: continue with the color of the outermost pixel,
        // but change alpha component to fully translucent
        int a = Utils.clamp(x, 0, img.getWidth() - 1);
        int b = Utils.clamp(y, 0, img.getHeight() - 1);
        int clr = img.getRGB(a, b);
        if (a == x && b == y)
            return clr;
        // keep color components, but set transparency to 0
        // (the idea is that border fades out and mixes with next tile)
        return clr & 0x00ffffff;
    }
}
