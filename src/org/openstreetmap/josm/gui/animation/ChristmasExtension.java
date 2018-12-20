// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Christmas animation extension. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public class ChristmasExtension implements AnimationExtension {

    ChristmasExtension() {
        this(0, 0);
    }

    private static final Random seed = new Random();
    private static final int averageStarWidth = 10;    // stars will be 5-15
    private static final int averageFallSpeed = 4;     // 2-6
    private static final int averageRotationSpeed = 2; // 1-3

    private static final Color WATER_LIVE_COLOR = new Color(80, 131, 160);

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
    public static double interpol(double origSize, double currentSize, double from, double to) {
        return (currentSize / origSize) * (to - from) + from;
    }

    private class Star {

        private int radiusX;
        private int radiusY;
        private int maxRadiusX;
        private int maxRadiusY;
        private int centerX;
        private int centerY;
        private final int fallSpeed;
        private final boolean orientation;
        private final int[] originalColor = new int[3];
        private final int[] color = new int[originalColor.length];
        private int direction;
        private final boolean haveEight;

        Star() {
            createRadiuses();
            haveEight = seed.nextBoolean();
            this.centerX = seed.nextInt(w + 1);
            this.centerY = seed.nextInt(h + 1);
            this.fallSpeed = averageFallSpeed / 2 + seed.nextInt(averageFallSpeed / 2);
            this.orientation = seed.nextBoolean();
            this.direction = -(averageRotationSpeed / 2 + seed.nextInt(averageRotationSpeed / 2));
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

        public void paint(Graphics g) {
            Color c = g.getColor();
            g.setColor(new Color(color[0], color[1], color[2]));
            Polygon p = createPolygon();
            if (haveEight) {
                int min1 = Math.min(radiusX, radiusY);
                int min2 = min1 / 2;
                g.fillRect(centerX - min2, centerY - min2, min1, min1);
            }
            g.fillPolygon(p);
            g.setColor(c);
        }

        private void animate() {
            centerY += fallSpeed;
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
            if (centerY > h + radiusX * 2 || centerY > h + radiusY * 2) {
                createRadiuses();
                this.centerX = seed.nextInt(w + 1);
                this.centerY = -radiusY * 2;
            }
        }

        private int createRadius() {
            return averageStarWidth / 2 + seed.nextInt(averageStarWidth);
        }

        private Polygon createPolygon() {
            int min = Math.min(radiusX, radiusY) / 3;
            Polygon p = new Polygon();
            p.addPoint(centerX - radiusX, centerY);
            p.addPoint(centerX - min, centerY - min);
            p.addPoint(centerX, centerY - radiusY);
            p.addPoint(centerX + min, centerY - min);
            p.addPoint(centerX + radiusX, centerY);
            p.addPoint(centerX + min, centerY + min);
            p.addPoint(centerX, centerY + radiusY);
            p.addPoint(centerX - min, centerY + min);
            return p;
        }

        private void interpolateColors(int is, int max) {
            for (int i = 0; i < originalColor.length; i++) {
                int fadeMin;
                if (centerY < 0) {
                    fadeMin = 0;
                } else if (centerY > h) {
                    fadeMin = 255;
                } else {
                    fadeMin = (int) interpol(h, centerY, 255, 0); // from white to black
                }
                int fadeMax;
                if (centerY < 0) {
                    fadeMax = 0;
                } else if (centerY > h) {
                    fadeMax = originalColor[i];
                } else {
                    fadeMax = (int) interpol(h, centerY, originalColor[i], 0); // from color to black
                }
                color[i] = (int) interpol(max, is, fadeMin, fadeMax);
            }
        }

        private void createRadiuses() {
            this.radiusX = createRadius();
            this.radiusY = radiusX;
            switch (seed.nextInt(3)) {
                case 0:
                    radiusX = radiusX + (2 * radiusX) / 3;
                    break;
                case 1:
                    radiusY = radiusY + (2 * radiusY) / 3;
                    break;
                case 2:
                    //noop
                    break;
            }
            maxRadiusX = radiusX;
            maxRadiusY = radiusY;
        }
    }

    private int w;
    private int h;
    private final List<Star> stars = new ArrayList<>(50);

    ChristmasExtension(int w, int h) {
        adjustForSize(w, h);
    }

    @Override
    public void paint(Graphics g) {
        stars.forEach(s -> s.paint(g));
    }

    @Override
    public void animate() {
        stars.forEach(Star::animate);
    }

    @Override
    public final void adjustForSize(int w, int h) {
        this.w = w;
        this.h = h;
        int count = w / (2 * (averageStarWidth + 1));
        while (stars.size() > count) {
            stars.remove(stars.size() - 1);
        }
        while (stars.size() < count) {
            stars.add(new Star());
        }
    }
}
