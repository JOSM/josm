// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Image warping algorithm.
 * <p>
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
    @FunctionalInterface
    public interface PointTransform {
        /**
         * Translates pixel coordinates.
         * @param x The x coordinate
         * @param y The y coordinate
         * @return transformed pixel coordinates
         */
        Point2D transform(double x, double y);
    }

    /**
     * Wrapper that optimizes a given {@link ImageWarp.PointTransform}.
     * <p>
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
        public Point2D transform(double x, double y) {
            int xIdx = (int) Math.floor(x / stride);
            int yIdx = (int) Math.floor(y / stride);
            double dx = x / stride - xIdx;
            double dy = y / stride - yIdx;
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
            final Map<Integer, Point2D> rowMap = getRow(yIdx);
            // This *was* computeIfAbsent. Unfortunately, it appears that it generated a ton of memory allocations.
            // As in, this was ~50 GB memory allocations in a test, and converting to a non-lambda form made it 1.3GB.
            // The primary culprit was LambdaForm#linkToTargetMethod
            Point2D current = rowMap.get(xIdx);
            if (current == null) {
                current = trfm.transform(xIdx * stride, yIdx * stride);
                rowMap.put(xIdx, current);
            }
            return current;
        }

        private Map<Integer, Point2D> getRow(int yIdx) {
            cleanUp(yIdx - 3);
            Map<Integer, Point2D> row = cache.get(yIdx);
            // Note: using computeIfAbsent will drastically increase memory allocations
            if (row == null) {
                row = new HashMap<>(256);
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
         * <p>
         * Simplest possible method. Faster, but not very good quality.
         */
        NEAREST_NEIGHBOR,

        /**
         * Bilinear.
         * <p>
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
        Objects.requireNonNull(interpolation, "interpolation");
        BufferedImage imgTarget = new BufferedImage(targetDim.width, targetDim.height, BufferedImage.TYPE_INT_ARGB);
        Rectangle2D srcRect = new Rectangle2D.Double(0, 0, srcImg.getWidth(), srcImg.getHeight());
        // These arrays reduce the amount of memory allocations (getRGB and setRGB are
        // collectively 40% of the memory cost, 78% if LambdaForm#linkToTargetMethod is
        // ignored). We mostly want to decrease GC pauses here.
        final int[] pixel = new int[1]; // Yes, this really does decrease memory allocations with TYPE_INT_ARGB.
        final Object sharedArray = getSharedArray(srcImg);
        for (int j = 0; j < imgTarget.getHeight(); j++) {
            for (int i = 0; i < imgTarget.getWidth(); i++) {
                Point2D srcCoord = invTransform.transform(i, j);
                if (srcRect.contains(srcCoord)) {
                    // Convert to switch expression when we switch to Java 17+.
                    int rgba = 0; // Initialized here so the compiler doesn't complain. Otherwise, BILINEAR needs to have it start at 0.
                    switch (interpolation) {
                        case NEAREST_NEIGHBOR:
                            rgba = getColor((int) Math.round(srcCoord.getX()), (int) Math.round(srcCoord.getY()), srcImg, sharedArray);
                            break;
                        case BILINEAR:
                            int x0 = (int) Math.floor(srcCoord.getX());
                            double dx = srcCoord.getX() - x0;
                            int y0 = (int) Math.floor(srcCoord.getY());
                            double dy = srcCoord.getY() - y0;
                            int c00 = getColor(x0, y0, srcImg, sharedArray);
                            int c01 = getColor(x0, y0 + 1, srcImg, sharedArray);
                            int c10 = getColor(x0 + 1, y0, srcImg, sharedArray);
                            int c11 = getColor(x0 + 1, y0 + 1, srcImg, sharedArray);
                            // rgba
                            // loop over color components: blue, green, red, alpha
                            for (int ch = 0; ch <= 3; ch++) {
                                int shift = 8 * ch;
                                int chVal = (int) Math.round(
                                    (((c00 >> shift) & 0xff) * (1-dx) + ((c10 >> shift) & 0xff) * dx) * (1-dy) +
                                    (((c01 >> shift) & 0xff) * (1-dx) + ((c11 >> shift) & 0xff) * dx) * dy);
                                rgba |= chVal << shift;
                            }
                            break;
                    }
                    imgTarget.getRaster().setDataElements(i, j, imgTarget.getColorModel().getDataElements(rgba, pixel));
                }
            }
        }
        return imgTarget;
    }

    private static Object getSharedArray(BufferedImage srcImg) {
        final int numBands = srcImg.getRaster().getNumBands();
        // Add data types as needed (shown via profiling, look for getRGB).
        switch (srcImg.getRaster().getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return new byte[numBands];
            case DataBuffer.TYPE_INT:
                return new int[numBands];
            default:
                return null;
        }
    }

    private static int getColor(int x, int y, BufferedImage img, Object sharedArray) {
        // border strategy: continue with the color of the outermost pixel,
        final int rx = Utils.clamp(x, 0, img.getWidth() - 1);
        final int ry = Utils.clamp(y, 0, img.getHeight() - 1);
        if (sharedArray == null) {
            return img.getRGB(rx, ry);
        }
        return img.getColorModel().getRGB(img.getRaster().getDataElements(rx, ry, sharedArray));
    }
}
