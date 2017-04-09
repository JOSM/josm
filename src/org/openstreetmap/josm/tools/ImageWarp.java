// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Image warping algorithm.
 *
 * Deforms an image geometrically according to a given transformation formula.
 */
public class ImageWarp {

    /**
     * Transformation that translates the pixel coordinates.
     */
    public static interface PointTransform {
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
                        int rgb;
                        switch (interpolation) {
                            case NEAREST_NEIGHBOR:
                                rgb = getColor((int) Math.round(srcCoord.getX()), (int) Math.round(srcCoord.getY()), srcImg).getRGB();
                                break;
                            case BILINEAR:
                                int x0 = (int) Math.floor(srcCoord.getX());
                                double dx = srcCoord.getX() - x0;
                                int y0 = (int) Math.floor(srcCoord.getY());
                                double dy = srcCoord.getY() - y0;
                                Color c00 = getColor(x0, y0, srcImg);
                                Color c01 = getColor(x0, y0 + 1, srcImg);
                                Color c10 = getColor(x0 + 1, y0, srcImg);
                                Color c11 = getColor(x0 + 1, y0 + 1, srcImg);
                                int red = (int) Math.round(
                                        (c00.getRed() * (1-dx) + c10.getRed() * dx) * (1-dy) +
                                        (c01.getRed() * (1-dx) + c11.getRed() * dx) * dy);
                                int green = (int) Math.round(
                                        (c00.getGreen()* (1-dx) + c10.getGreen() * dx) * (1-dy) +
                                        (c01.getGreen() * (1-dx) + c11.getGreen() * dx) * dy);
                                int blue = (int) Math.round(
                                        (c00.getBlue()* (1-dx) + c10.getBlue() * dx) * (1-dy) +
                                        (c01.getBlue() * (1-dx) + c11.getBlue() * dx) * dy);
                                int alpha = (int) Math.round(
                                        (c00.getAlpha()* (1-dx) + c10.getAlpha() * dx) * (1-dy) +
                                        (c01.getAlpha() * (1-dx) + c11.getAlpha() * dx) * dy);
                                rgb = new Color(red, green, blue, alpha).getRGB();
                                break;
                            default:
                                throw new AssertionError();
                        }
                        imgTarget.setRGB(i, j, rgb);
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

    private static Color getColor(int x, int y, BufferedImage img) {
        // border strategy: continue with the color of the outermost pixel,
        // but change alpha component to fully translucent
        boolean transparent = false;
        if (x < 0) {
            x = 0;
            transparent = true;
        } else if (x >= img.getWidth()) {
            x = img.getWidth() - 1;
            transparent = true;
        }
        if (y < 0) {
            y = 0;
            transparent = true;
        } else if (y >= img.getHeight()) {
            y = img.getHeight() - 1;
            transparent = true;
        }
        Color clr = new Color(img.getRGB(x, y));
        if (!transparent)
            return clr;
        // keep color components, but set transparency to 0
        // (the idea is that border fades out and mixes with next tile)
        return new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), 0);
    }
}
