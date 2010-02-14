// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static java.awt.geom.Rectangle2D.OUT_LEFT;
import static java.awt.geom.Rectangle2D.OUT_RIGHT;
import static java.awt.geom.Rectangle2D.OUT_TOP;
import static java.awt.geom.Rectangle2D.OUT_BOTTOM;
import java.awt.Point;

/**
 * Computes the part of a line that is visible in a given rectangle.
 * Using int leads to overflow, so we need long int.
 * http://en.wikipedia.org/wiki/Cohen-Sutherland
 */
public class LineClip {
    private Point p1, p2;

    /**
     * The outcode of the point.
     * We cannot use Rectangle.outcode since it does not work with long ints.
     */
    public int computeOutCode (long x, long y, long xmin, long ymin, long xmax, long ymax) {
        int code = 0;
        if (y > ymax) {
            code |= OUT_TOP;
        }
        else if (y < ymin) {
            code |= OUT_BOTTOM;
        }
        if (x > xmax) {
            code |= OUT_RIGHT;
        }
        else if (x < xmin) {
            code |= OUT_LEFT;
        }
        return code;
    }

    public boolean cohenSutherland( long x1, long y1, long x2, long y2, long xmin, long ymin, long xmax, long ymax)
    {
        int outcode0, outcode1, outcodeOut;
        boolean accept = false;
        boolean done = false;

        outcode0 = computeOutCode (x1, y1, xmin, ymin, xmax, ymax);
        outcode1 = computeOutCode (x2, y2, xmin, ymin, xmax, ymax);

        do {
            if ((outcode0 | outcode1) == 0 ) {
                accept = true;
                done = true;
            }
            else if ( (outcode0 & outcode1) > 0 ) {
                done = true;
            }
            else {
                long x = 0, y = 0;
                outcodeOut = outcode0 != 0 ? outcode0: outcode1;
                if ( (outcodeOut & OUT_TOP) > 0 ) {
                    x = x1 + (x2 - x1) * (ymax - y1)/(y2 - y1);
                    y = ymax;
                }
                else if ((outcodeOut & OUT_BOTTOM) > 0 ) {
                    x = x1 + (x2 - x1) * (ymin - y1)/(y2 - y1);
                    y = ymin;
                }
                else if ((outcodeOut & OUT_RIGHT)> 0) {
                    y = y1 + (y2 - y1) * (xmax - x1)/(x2 - x1);
                    x = xmax;
                }
                else if ((outcodeOut & OUT_LEFT) > 0) {
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

        if(accept) {
            p1 = new Point((int) x1, (int) y1);
            p2 = new Point((int) x2, (int) y2);
            return true;
        }
        return false;
    }

    public Point getP1()
    {
        return p1;
    }

    public Point getP2()
    {
        return p2;
    }
}
