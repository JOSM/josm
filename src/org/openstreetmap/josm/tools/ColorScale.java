// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Utility class that helps to work with color scale for coloring GPX tracks etc.
 * @since 7319
 */
public final class ColorScale {
    private double min, max;
    private Color noDataColor;
    private Color belowMinColor;
    private Color aboveMaxColor;

    private Color[] colors;
    private String title = "";
    private int intervalCount = 5;

    private ColorScale() {

    }

    public static ColorScale createHSBScale(int count) {
        ColorScale sc = new ColorScale();
        sc.colors = new Color[count];
        for (int i = 0; i < count; i++) {
            sc.colors[i] = Color.getHSBColor(i / 300.0f, 1, 1);
        }
        sc.setRange(0, 255);
        sc.addBounds();
        return sc;
    }

    public static ColorScale createCyclicScale(int count) {
        ColorScale sc = new ColorScale();
        //                    red   yellow  green   blue    red
        int[] h = new int[] {0,    59,     127,    244,    360};
        int[] s = new int[] {100,  84,     99,     100};
        int[] b = new int[] {90,   93,     74,     83};

        sc.colors = new Color[count];
        for (int i = 0; i < sc.colors.length; i++) {

            float angle = 4 - i / 256f * 4;
            int quadrant = (int) angle;
            angle -= quadrant;
            quadrant = Utils.mod(quadrant+1, 4);

            float vh = h[quadrant] * w(angle) + h[quadrant+1] * (1 - w(angle));
            float vs = s[quadrant] * w(angle) + s[Utils.mod(quadrant+1, 4)] * (1 - w(angle));
            float vb = b[quadrant] * w(angle) + b[Utils.mod(quadrant+1, 4)] * (1 - w(angle));

            sc.colors[i] = Color.getHSBColor(vh/360f, vs/100f, vb/100f);
        }
        sc.setRange(0, 2*Math.PI);
        sc.addBounds();
        return sc;
    }

    /**
     * transition function:
     *  w(0)=1, w(1)=0, 0&lt;=w(x)&lt;=1
     * @param x number: 0&lt;=x&lt;=1
     * @return the weighted value
     */
    private static float w(float x) {
        if (x < 0.5)
            return 1 - 2*x*x;
        else
            return 2*(1-x)*(1-x);
    }

    public void setRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Add standard colors for values below min or above max value
     */
    public void addBounds() {
        aboveMaxColor = colors[colors.length-1];
        belowMinColor = colors[0];
    }

    public final Color getColor(double value) {
        if (value < min) return belowMinColor;
        if (value > max) return aboveMaxColor;
        if (Double.isNaN(value)) return noDataColor;
        final int n = colors.length;
        int idx = (int) ((value-min)*colors.length / (max-min));
        if (idx < colors.length) {
            return colors[idx];
        } else {
            return colors[n-1]; // this happens when value==max
        }
    }

    public final Color getColor(Number value) {
        return (value == null) ? noDataColor : getColor(value.doubleValue());
    }

    public Color getNoDataColor() {
        return noDataColor;
    }

    public void setNoDataColor(Color noDataColor) {
        this.noDataColor = noDataColor;
    }

    public ColorScale makeTransparent(int alpha) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Color((colors[i].getRGB() & 0xFFFFFF) | ((alpha & 0xFF) << 24), true);
        }
        return this;
    }

    public ColorScale addTitle(String title) {
        this.title = title;
        return this;
    }

    public ColorScale setIntervalCount(int intervalCount) {
        this.intervalCount = intervalCount;
        return this;
    }

    public ColorScale makeReversed() {
        int n = colors.length;
        Color tmp;
        for (int i = 0; i < n/2; i++) {
            tmp = colors[i];
            colors[i] = colors[n-1-i];
            colors[n-1-i] = tmp;
        }
        tmp = belowMinColor;
        belowMinColor = aboveMaxColor;
        aboveMaxColor = tmp;
        return this;
    }

    public void drawColorBar(Graphics2D g, int x, int y, int w, int h, double valueScale) {
        int n = colors.length;

        for (int i = 0; i < n; i++) {
            g.setColor(colors[i]);
            if (w < h) {
                g.fillRect(x, y+i*h/n, w, h/n+1);
            } else {
                g.fillRect(x+i*w/n, y, w/n+1, h);
            }
        }

        int fw, fh;
        FontMetrics fm = g.getFontMetrics();
        fh = fm.getHeight()/2;
        fw = fm.stringWidth(String.valueOf(Math.max((int) Math.abs(max*valueScale),
                (int) Math.abs(min*valueScale)))) + fm.stringWidth("0.123");
        g.setColor(noDataColor);
        if (title != null) {
            g.drawString(title, x-fw-3, y-fh*3/2);
        }
        for (int i = 0; i <= intervalCount; i++) {
            g.setColor(colors[(int) (1.0*i*n/intervalCount-1e-10)]);
            final double val =  min+i*(max-min)/intervalCount;
            final String txt = String.format("%.3f", val*valueScale);
            if (w < h) {
                g.drawString(txt, x-fw-3, y+i*h/intervalCount+fh/2);
            } else {
                g.drawString(txt, x+i*w/intervalCount-fw/2, y+fh-3);
            }
        }
    }
}
