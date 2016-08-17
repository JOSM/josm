// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static java.awt.geom.Rectangle2D.OUT_BOTTOM;
import static java.awt.geom.Rectangle2D.OUT_LEFT;
import static java.awt.geom.Rectangle2D.OUT_RIGHT;
import static java.awt.geom.Rectangle2D.OUT_TOP;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Computes the part of a line that is visible in a given rectangle.
 * Using int leads to overflow, so we need long int.
 */
public class LineClip {
    private Point2D p1, p2;
    private final Rectangle2D clipBounds;

    /**
     * Constructs a new {@code LineClip}.
     * @param p1 start point of the clipped line
     * @param p2 end point of the clipped line
     * @param clipBounds Clip bounds
     * @since 10827
     */
    public LineClip(Point2D p1, Point2D p2, Rectangle2D clipBounds) {
        this.p1 = p1;
        this.p2 = p2;
        this.clipBounds = clipBounds;
    }

    /**
     * run the clipping algorithm
     * @return true if the some parts of the line lies within the clip bounds
     */
    public boolean execute() {
        if (clipBounds == null) {
            return false;
        }
        return cohenSutherland(p1.getX(), p1.getY(), p2.getX(), p2.getY(), clipBounds.getMinX(), clipBounds.getMinY(),
                clipBounds.getMaxX(), clipBounds.getMaxY());
    }

    /**
     * @return start point of the clipped line
     * @since 10827
     */
    public Point2D getP1() {
        return p1;
    }

    /**
     * @return end point of the clipped line
     * @since 10827
     */
    public Point2D getP2() {
        return p2;
    }

    /**
     * Cohen–Sutherland algorithm.
     * See <a href="https://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm">Wikipedia article</a>
     * @param x1 X coordinate of first point
     * @param y1 Y coordinate of first point
     * @param x2 X coordinate of second point
     * @param y2 Y coordinate of second point
     * @param xmin minimal X coordinate
     * @param ymin minimal Y coordinate
     * @param xmax maximal X coordinate
     * @param ymax maximal Y coordinate
     * @return true, if line is visible in the given clip region
     */
    private boolean cohenSutherland(double x1, double y1, double x2, double y2, double xmin, double ymin, double xmax, double ymax) {
        int outcode0, outcode1, outcodeOut;
        boolean accept = false;
        boolean done = false;

        outcode0 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
        outcode1 = computeOutCode(x2, y2, xmin, ymin, xmax, ymax);

        do {
            if ((outcode0 | outcode1) == 0) {
                accept = true;
                done = true;
            } else if ((outcode0 & outcode1) > 0) {
                done = true;
            } else {
                double x = 0;
                double y = 0;
                outcodeOut = outcode0 != 0 ? outcode0 : outcode1;
                if ((outcodeOut & OUT_TOP) != 0) {
                    x = x1 + (x2 - x1) * (ymax - y1)/(y2 - y1);
                    y = ymax;
                } else if ((outcodeOut & OUT_BOTTOM) != 0) {
                    x = x1 + (x2 - x1) * (ymin - y1)/(y2 - y1);
                    y = ymin;
                } else if ((outcodeOut & OUT_RIGHT) != 0) {
                    y = y1 + (y2 - y1) * (xmax - x1)/(x2 - x1);
                    x = xmax;
                } else if ((outcodeOut & OUT_LEFT) != 0) {
                    y = y1 + (y2 - y1) * (xmin - x1)/(x2 - x1);
                    x = xmin;
                }
                if (outcodeOut == outcode0) {
                    x1 = x;
                    y1 = y;
                    outcode0 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
                } else {
                    x2 = x;
                    y2 = y;
                    outcode1 = computeOutCode(x2, y2, xmin, ymin, xmax, ymax);
                }
            }
        }
        while (!done);

        if (accept) {
            p1 = new Point2D.Double(x1, y1);
            p2 = new Point2D.Double(x2, y2);
            return true;
        }
        return false;
    }

    /**
     * The outcode of the point.
     * We cannot use {@link Rectangle#outcode} since it does not work with long ints.
     * @param x X coordinate
     * @param y Y coordinate
     * @param xmin minimal X coordinate
     * @param ymin minimal Y coordinate
     * @param xmax maximal X coordinate
     * @param ymax maximal Y coordinate
     * @return outcode
     */
    private static int computeOutCode(double x, double y, double xmin, double ymin, double xmax, double ymax) {
        int code = 0;
        // ignore rounding errors.
        if (y > ymax + 1e-10) {
            code |= OUT_TOP;
        } else if (y < ymin - 1e-10) {
            code |= OUT_BOTTOM;
        }
        if (x > xmax + 1e-10) {
            code |= OUT_RIGHT;
        } else if (x < xmin - 1e-10) {
            code |= OUT_LEFT;
        }
        return code;
    }
}
