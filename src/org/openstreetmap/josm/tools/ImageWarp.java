// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Image warping algorithm.
 *
 * Deforms an image geometrically according to a given transformation formula.
 * @since 11858
 */
public final class ImageWarp {

    private ImageWarp() {
        // Hide default constructor
    }

    /**
     * Transformation that translates the pixel coordinates.
     */
    public interface PointTransform {
        /**
         * Translates pixel coordinates.
         * @param pt pixel coordinates
         * @return transformed pixel coordinates
         */
        Point2D transform(Point2D pt);
    }

    /**
     * Wrapper that optimizes a given {@link ImageWarp.PointTransform}.
     *
     * It does so by spanning a grid with certain step size. It will invoke the
     * potentially expensive master transform only at those grid points and use
     * bilinear interpolation to approximate transformed values in between.
     * <p>
     * For memory optimization, this class assumes that rows are more or less scanned
     * one-by-one as is done in {@link ImageWarp#warp}. I.e. this transform is <em>not</em>
     * random access in the y coordinate.
     */
    public static class GridTransform implements ImageWarp.PointTransform {

        private final double stride;
        private final ImageWarp.PointTransform trfm;

        private final Map<Integer, Map<Integer, Point2D>> cache;

        private final boolean consistencyTest;
        private final Set<Integer> deletedRows;

        /**
         * Create a new GridTransform.
         * @param trfm the master transform, that needs to be optimized
         * @param stride step size
         */
        public GridTransform(ImageWarp.PointTransform trfm, double stride) {
            this.trfm = trfm;
            this.stride = stride;
            this.cache = new HashMap<>();
            this.consistencyTest = Logging.isDebugEnabled();
            if (consistencyTest) {
                deletedRows = new HashSet<>();
            } else {
                deletedRows = null;
            }
        }

        @Override
        public Point2D transform(Point2D pt) {
            int xIdx = (int) Math.floor(pt.getX() / stride);
            int yIdx = (int) Math.floor(pt.getY() / stride);
            double dx = pt.getX() / stride - xIdx;
            double dy = pt.getY() / stride - yIdx;
            Point2D value00 = getValue(xIdx, yIdx);
            Point2D value01 = getValue(xIdx, yIdx + 1);
            Point2D value10 = getValue(xIdx + 1, yIdx);
            Point2D value11 = getValue(xIdx + 1, yIdx + 1);
            double valueX = (value00.getX() * (1-dx) + value10.getX() * dx) * (1-dy) +
                    (value01.getX() * (1-dx) + value11.getX() * dx) * dy;
            double valueY = (value00.getY() * (1-dx) + value10.getY() * dx) * (1-dy) +
                    (value01.getY() * (1-dx) + value11.getY() * dx) * dy;
            return new Point2D.Double(valueX, valueY);
        }

        private Point2D getValue(int xIdx, int yIdx) {
            return getRow(yIdx).computeIfAbsent(xIdx, k -> trfm.transform(new Point2D.Double(xIdx * stride, yIdx * stride)));
        }

        private Map<Integer, Point2D> getRow(int yIdx) {
            cleanUp(yIdx - 3);
            Map<Integer, Point2D> row = cache.get(yIdx);
            if (row == null) {
                row = new HashMap<>();
                cache.put(yIdx, row);
                if (consistencyTest) {
                    // should not create a row that has been deleted before
                    if (deletedRows.contains(yIdx)) throw new AssertionError();
                    // only ever cache 3 rows at once
                    if (cache.size() > 3) throw new AssertionError();
                }
            }
            return row;
        }

        // remove rows from cache that will no longer be used
        private void cleanUp(int yIdx) {
            Map<Integer, Point2D> del = cache.remove(yIdx);
            if (consistencyTest && del != null) {
                // should delete each row only once
                if (deletedRows.contains(yIdx)) throw new AssertionError();
                deletedRows.add(yIdx);
            }
        }
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
        NEAREST_NEIGHBOR,

        /**
         * Bilinear.
         *
         * Decent quality.
         */
        BILINEAR;
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
                if (srcRect.contains(srcCoord)) {
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
                            throw new AssertionError(Objects.toString(interpolation));
                    }
                    imgTarget.setRGB(i, j, rgba);
                }
            }
        }
        return imgTarget;
    }

    private static int getColor(int x, int y, BufferedImage img) {
        // border strategy: continue with the color of the outermost pixel,
        return img.getRGB(
                Utils.clamp(x, 0, img.getWidth() - 1),
                Utils.clamp(y, 0, img.getHeight() - 1));
    }
}
