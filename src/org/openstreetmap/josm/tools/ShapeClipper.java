// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Tools to clip a shape based on the Sutherland-Hodgman algorithm.
 * See https://en.wikipedia.org/wiki/Sutherland%E2%80%93Hodgman_algorithm
 * @author Gerd Petermann
 * @since 14583
 */
public final class ShapeClipper {
    private static final int LEFT = 0;
    private static final int TOP = 1;
    private static final int RIGHT = 2;
    private static final int BOTTOM = 3;

    private ShapeClipper() {
        // Hide default constructor for util classes
    }

    /**
     * Clip a given (closed) shape with a given rectangle.
     * @param shape the subject shape to clip
     * @param clippingRect the clipping rectangle
     * @return the intersection of the shape and the rectangle
     * or null if they don't intersect or the shape is not closed.
     * The intersection may contain dangling edges.
     */
    public static Path2D.Double clipShape(Shape shape, Rectangle2D clippingRect) {
        Path2D.Double result = new Path2D.Double();
        boolean hasData = false;
        double minX, minY, maxX, maxY;
        int num = 0;
        minX = minY = Double.POSITIVE_INFINITY;
        maxX = maxY = Double.NEGATIVE_INFINITY;

        PathIterator pit = shape.getPathIterator(null);
        double[] points = new double[512];
        double[] res = new double[6];
        while (!pit.isDone()) {
            int type = pit.currentSegment(res);
            if (num > 0 && (type == PathIterator.SEG_CLOSE || type == PathIterator.SEG_MOVETO || pit.isDone())) {
                // we have extracted a single segment, maybe unclosed
                hasData |= addToResult(result, points, num,
                        new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), clippingRect);
                num = 0;
                minX = minY = Double.POSITIVE_INFINITY;
                maxX = maxY = Double.NEGATIVE_INFINITY;
            }
            double x = res[0];
            double y = res[1];
            if (x < minX)
                minX = x;
            if (x > maxX)
                maxX = x;
            if (y < minY)
                minY = y;
            if (y > maxY)
                maxY = y;
            if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_MOVETO) {
                if (num + 2 >= points.length) {
                    points = Arrays.copyOf(points, points.length * 2);
                }
                points[num++] = x;
                points[num++] = y;
                // } else if (type != PathIterator.SEG_CLOSE) {
                //Logging.warn("unhandled path iterator");
            }
            pit.next();
        }
        if (num > 2) {
            // we get here if last segment was not closed
            hasData |= addToResult(result, points, num,
                    new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), clippingRect);
        }
        return hasData ? result : null;
    }

    /**
     * Clip extracted segment if needed and add it to result if not completely outside of clipping rectangle.
     * @param result the path that will describe the clipped shape (modified)
     * @param points array of x/y pairs
     * @param num the number of valid values in points
     * @param bbox the bounding box of the path
     * @param clippingRect the clipping rectangle
     * @return true if data was added to result
     */
    private static boolean addToResult(Path2D.Double result, double[] points, int num,
            Rectangle2D bbox, Rectangle2D clippingRect) {
        Path2D.Double segment = null;
        if (clippingRect.contains(bbox)) {
            // all points are inside clipping rectangle
            segment = pointsToPath2D(points, num);
        } else {
            segment = clipSinglePathWithSutherlandHodgman(points, num, bbox, clippingRect);
        }
        if (segment != null) {
            result.append(segment, false);
            return true;
        }
        return false;
    }

    /**
     * Convert a list of points to a Path2D.Double
     * @param points array of x/y pairs
     * @param num the number of valid values in points
     * @return the path or null if the path describes a point or line.
     */
    private static Path2D.Double pointsToPath2D(double[] points, int num) {
        if (num < 2)
            return null;
        if (Double.compare(points[0], points[num - 2]) == 0 && Double.compare(points[1], points[num - 1]) == 0) {
            num -= 2;
        }
        if (num < 6)
            return null;
        Path2D.Double path = new Path2D.Double();
        double lastX = points[0], lastY = points[1];
        path.moveTo(lastX, lastY);
        int numOut = 1;
        for (int i = 2; i < num;) {
            double x = points[i++], y = points[i++];
            if (x != lastX || y != lastY) {
                path.lineTo(x, y);
                lastX = x;
                lastY = y;
                ++numOut;
            }
        }
        if (numOut < 3)
            return null;
        return path;
    }

    /**
     * Clip a single path with a given rectangle using the Sutherland-Hodgman algorithm. This is much faster compared to
     * the area.intersect method, but may create dangling edges.
     * @param points array of x/y pairs
     * @param num the number of valid values in points
     * @param bbox the bounding box of the path
     * @param clippingRect the clipping rectangle
     * @return the clipped path as a Path2D.Double or null if the result is empty
     */
    private static Path2D.Double clipSinglePathWithSutherlandHodgman(double[] points, int num, Rectangle2D bbox,
            Rectangle2D clippingRect) {
        if (num <= 2 || !bbox.intersects(clippingRect)) {
            return null;
        }

        int countVals = num;
        if (Double.compare(points[0], points[num - 2]) == 0 && Double.compare(points[1], points[num - 1]) == 0) {
            countVals -= 2;
        }

        double[] outputList = points;
        double[] input;

        double leftX = clippingRect.getMinX();
        double rightX = clippingRect.getMaxX();
        double lowerY = clippingRect.getMinY();
        double upperY = clippingRect.getMaxY();
        boolean eIsIn = false, sIsIn = false;
        for (int side = LEFT; side <= BOTTOM; side++) {
            if (countVals < 6)
                return null; // ignore point or line

            boolean skipTestForThisSide;
            switch (side) {
            case LEFT:
                skipTestForThisSide = (bbox.getMinX() >= leftX);
                break;
            case TOP:
                skipTestForThisSide = (bbox.getMaxY() < upperY);
                break;
            case RIGHT:
                skipTestForThisSide = (bbox.getMaxX() < rightX);
                break;
            default:
                skipTestForThisSide = (bbox.getMinY() >= lowerY);
            }
            if (skipTestForThisSide)
                continue;

            input = outputList;
            outputList = new double[countVals + 16];
            double sLon = 0, sLat = 0;
            double pLon = 0, pLat = 0; // intersection
            int posIn = countVals - 2;
            int posOut = 0;
            for (int i = 0; i < countVals + 2; i += 2) {
                if (posIn >= countVals)
                    posIn = 0;
                double eLon = input[posIn++];
                double eLat = input[posIn++];
                switch (side) {
                case LEFT:
                    eIsIn = (eLon >= leftX);
                    break;
                case TOP:
                    eIsIn = (eLat < upperY);
                    break;
                case RIGHT:
                    eIsIn = (eLon < rightX);
                    break;
                default:
                    eIsIn = (eLat >= lowerY);
                }
                if (i > 0) {
                    if (eIsIn != sIsIn) {
                        // compute intersection
                        double slope;
                        if (eLon != sLon)
                            slope = (eLat - sLat) / (eLon - sLon);
                        else
                            slope = 1;

                        switch (side) {
                        case LEFT:
                            pLon = leftX;
                            pLat = slope * (leftX - sLon) + sLat;
                            break;
                        case RIGHT:
                            pLon = rightX;
                            pLat = slope * (rightX - sLon) + sLat;
                            break;

                        case TOP:
                            if (eLon != sLon)
                                pLon = sLon + (upperY - sLat) / slope;
                            else
                                pLon = sLon;
                            pLat = upperY;
                            break;
                        default: // BOTTOM
                            if (eLon != sLon)
                                pLon = sLon + (lowerY - sLat) / slope;
                            else
                                pLon = sLon;
                            pLat = lowerY;
                            break;

                        }
                    }
                    int toAdd = 0;
                    if (eIsIn) {
                        if (!sIsIn) {
                            toAdd += 2;
                        }
                        toAdd += 2;
                    } else {
                        if (sIsIn) {
                            toAdd += 2;
                        }
                    }
                    if (posOut + toAdd >= outputList.length) {
                        // unlikely
                        outputList = Arrays.copyOf(outputList, outputList.length * 2);
                    }
                    if (eIsIn) {
                        if (!sIsIn) {
                            outputList[posOut++] = pLon;
                            outputList[posOut++] = pLat;
                        }
                        outputList[posOut++] = eLon;
                        outputList[posOut++] = eLat;
                    } else {
                        if (sIsIn) {
                            outputList[posOut++] = pLon;
                            outputList[posOut++] = pLat;
                        }
                    }
                }
                // S = E
                sLon = eLon;
                sLat = eLat;
                sIsIn = eIsIn;
            }
            countVals = posOut;
        }
        return pointsToPath2D(outputList, countVals);
    }
}
