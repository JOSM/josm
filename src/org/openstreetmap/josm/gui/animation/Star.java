// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.util.Random;

/**
 * A star displayed when {@link ChristmasExtension} is active. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14581
 */
class Star {
    private static final Random seed = new Random();

    static final int averageStarWidth = 10;    // stars will be 5-15
    static final int averageFallSpeed = 4;     // 2-6
    static final int averageRotationSpeed = 2; // 1-3

    private static final Color WATER_LIVE_COLOR = new Color(80, 131, 160);

    private final int w;
    private final int h;

    private int radiusX;
    private int radiusY;
    private int maxRadiusX;
    private int maxRadiusY;
    private final Point center = new Point();
    private final int fallSpeed;
    private final boolean orientation;
    private final int[] originalColor = new int[3];
    private final int[] color = new int[originalColor.length];
    private int direction;
    private final boolean haveEight;

    Star(int w, int h) {
        this.w = w;
        this.h = h;
        createRadiuses();
        haveEight = seed.nextBoolean();
        center.x = seed.nextInt(w + 1);
        center.y = seed.nextInt(h + 1);
        fallSpeed = averageFallSpeed / 2 + seed.nextInt(averageFallSpeed / 2);
        orientation = seed.nextBoolean();
        direction = -(averageRotationSpeed / 2 + seed.nextInt(averageRotationSpeed / 2));
        if (seed.nextInt(4) == 0) {
            originalColor[0] = Color.yellow.getRed();
            originalColor[1] = Color.yellow.getGreen();
            originalColor[2] = Color.yellow.getBlue();
        } else {
            originalColor[0] = WATER_LIVE_COLOR.getRed();
            originalColor[1] = WATER_LIVE_COLOR.getGreen();
            originalColor[2] = WATER_LIVE_COLOR.getBlue();
        }
    }

    void paint(Graphics g) {
        Color c = g.getColor();
        g.setColor(new Color(color[0], color[1], color[2]));
        Polygon p = createPolygon();
        if (haveEight) {
            int min1 = Math.min(radiusX, radiusY);
            int min2 = min1 / 2;
            g.fillRect(center.x - min2, center.y - min2, min1, min1);
        }
        g.fillPolygon(p);
        g.setColor(c);
    }

    void animate() {
        center.y += fallSpeed;
        if (orientation) {
            radiusX += direction;
            if (radiusX <= -direction) {
                direction = -direction;
                radiusX = direction;
            }
            if (radiusX >= maxRadiusX) {
                direction = -direction;
                radiusX = maxRadiusX;
            }
            interpolateColors(radiusX, maxRadiusX);
        } else {
            radiusY += direction;
            if (radiusY <= -direction) {
                direction = -direction;
                radiusY = direction;
            }
            if (radiusY >= maxRadiusY) {
                direction = -direction;
                radiusY = maxRadiusY;
            }
            interpolateColors(radiusY, maxRadiusY);
        }
        if (center.y > h + radiusX * 2 || center.y > h + radiusY * 2) {
            createRadiuses();
            center.x = seed.nextInt(w + 1);
            center.y = -radiusY * 2;
        }
    }

    private static int createRadius() {
        return averageStarWidth / 2 + seed.nextInt(averageStarWidth);
    }

    private Polygon createPolygon() {
        int min = Math.min(radiusX, radiusY) / 3;
        Polygon p = new Polygon();
        p.addPoint(center.x - radiusX, center.y);
        p.addPoint(center.x - min, center.y - min);
        p.addPoint(center.x, center.y - radiusY);
        p.addPoint(center.x + min, center.y - min);
        p.addPoint(center.x + radiusX, center.y);
        p.addPoint(center.x + min, center.y + min);
        p.addPoint(center.x, center.y + radiusY);
        p.addPoint(center.x - min, center.y + min);
        return p;
    }

    private void interpolateColors(int is, int max) {
        for (int i = 0; i < originalColor.length; i++) {
            int fadeMin;
            if (center.y < 0) {
                fadeMin = 0;
            } else if (center.y > h) {
                fadeMin = 255;
            } else {
                fadeMin = (int) interpol(h, center.y, 255, 0); // from white to black
            }
            int fadeMax;
            if (center.y < 0) {
                fadeMax = 0;
            } else if (center.y > h) {
                fadeMax = originalColor[i];
            } else {
                fadeMax = (int) interpol(h, center.y, originalColor[i], 0); // from color to black
            }
            color[i] = (int) interpol(max, is, fadeMin, fadeMax);
        }
    }

    private void createRadiuses() {
        radiusX = createRadius();
        radiusY = radiusX;
        switch (seed.nextInt(3)) {
            case 0:
                radiusX = radiusX + (2 * radiusX) / 3;
                break;
            case 1:
                radiusY = radiusY + (2 * radiusY) / 3;
                break;
            default:
                break;
        }
        maxRadiusX = radiusX;
        maxRadiusY = radiusY;
    }

    /**
     * Interpolation is root ratio is r= (currentSize / origSize)
     * then value to-from is interpolated from to to from according to ratio
     *
     * @param origSize original size
     * @param currentSize current size
     * @param from starting value
     * @param to ending value
     * @return interpolated value
     */
    static double interpol(double origSize, double currentSize, double from, double to) {
        return (currentSize / origSize) * (to - from) + from;
    }
}
