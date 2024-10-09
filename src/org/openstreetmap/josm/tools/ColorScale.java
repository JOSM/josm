// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.util.Arrays;
import java.util.Date;
import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.openstreetmap.josm.data.preferences.NamedColorProperty;

import javax.swing.UIManager;

/**
 * Utility class that helps to work with color scale for coloring GPX tracks etc.
 * @since 7319
 */
public final class ColorScale {
    private static final Color LEGEND_BACKGROUND = new NamedColorProperty(marktr("gpx legend background"), new Color(180, 180, 180, 160)).get();
    private static final Color LEGEND_TEXT_OUTLINE_DARK = new NamedColorProperty(marktr("gpx legend text outline dark"),
            new Color(102, 102, 102)).get();
    private static final Color LEGEND_TEXT_OUTLINE_BRIGHT = new NamedColorProperty(marktr("gpx legend text outline bright"),
            new Color(204, 204, 204)).get();
    private static final Color LEGEND_TITLE = new NamedColorProperty(marktr("gpx legend title color"), new Color(0, 0, 0)).get();

    private static final String DAY_TIME_FORMAT = "yyyy-MM-dd      HH:mm";
    private static final String TIME_FORMAT = "HH:mm:ss";
    /** Padding for the legend (from the text to the edge of the rectangle) */
    private static final byte PADDING = 19;

    private double min, max;
    private Color noDataColor;
    private Color belowMinColor;
    private Color aboveMaxColor;

    private Color[] colors;
    private String[] colorBarTitles;
    private String title = "";
    private int intervalCount = 5;

    private ColorScale() {

    }

    /**
     * Gets a fixed color range.
     * @param colors the fixed colors list
     * @return The scale
     * @since 15247
     */
    public static ColorScale createFixedScale(Color[] colors) {
        ColorScale sc = new ColorScale();
        sc.colors = Utils.copyArray(colors);
        sc.setRange(0, colors.length - 1d);
        sc.addBounds();
        return sc;
    }

    /**
     * Gets a HSB color range.
     * @param count The number of colors the scale should have
     * @return The scale
     */
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

    /**
     * Creates a cyclic color scale (red  yellow  green   blue    red)
     * @param count The number of colors the scale should have
     * @return The scale
     */
    public static ColorScale createCyclicScale(int count) {
        ColorScale sc = new ColorScale();
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        //                   red  yellow  green   blue    red
        int[] h = {0,    59,     127,    244,   360};
        int[] s = {100,  84,     99,     100};
        int[] b = {90,   93,     74,     83};
        // CHECKSTYLE.ON: SingleSpaceSeparator

        sc.colors = new Color[count];
        for (int i = 0; i < sc.colors.length; i++) {

            float angle = i * 4f / count;
            int quadrant = (int) angle;
            angle -= quadrant;
            quadrant = Utils.mod(quadrant+1, 4);

            float vh = h[quadrant] * weighted(angle) + h[quadrant+1] * (1 - weighted(angle));
            float vs = s[quadrant] * weighted(angle) + s[Utils.mod(quadrant+1, 4)] * (1 - weighted(angle));
            float vb = b[quadrant] * weighted(angle) + b[Utils.mod(quadrant+1, 4)] * (1 - weighted(angle));

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
    private static float weighted(float x) {
        if (x < 0.5)
            return 1 - 2*x*x;
        else
            return 2*(1-x)*(1-x);
    }

    /**
     * Sets the hint on the range this scale is for
     * @param min The minimum value
     * @param max The maximum value
     */
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

    /**
     * Gets a color for the given value.
     * @param value The value
     * @return The color for this value, this may be a special color if the value is outside the range but never null.
     */
    public Color getColor(double value) {
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

    /**
     * Gets a color for the given value.
     * @param value The value, may be <code>null</code>
     * @return The color for this value, this may be a special color if the value is outside the range or the value is null but never null.
     */
    public Color getColor(Number value) {
        return (value == null) ? noDataColor : getColor(value.doubleValue());
    }

    /**
     * Get the color to use if there is no data
     * @return The color
     */
    public Color getNoDataColor() {
        return noDataColor;
    }

    /**
     * Sets the color to use if there is no data
     * @param noDataColor The color
     */
    public void setNoDataColor(Color noDataColor) {
        this.noDataColor = noDataColor;
    }

    /**
     * Make all colors transparent
     * @param alpha The alpha value all colors in the range should have, range 0..255
     * @return This scale, for chaining
     */
    public ColorScale makeTransparent(int alpha) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Color((colors[i].getRGB() & 0xFFFFFF) | ((alpha & 0xFF) << 24), true);
        }
        return this;
    }

    /**
     * Adds a title to this scale
     * @param title The new title
     * @return This scale, for chaining
     */
    public ColorScale addTitle(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }

    /**
     * Adds titles to the color bar for a fixed scale
     * @param titles Array of String, same length as the colors array
     * @return This scale, for chaining
     * @since 18396
     */
    public ColorScale addColorBarTitles(String[] titles) {
        this.intervalCount = titles.length - 1;
        this.colorBarTitles = titles;
        return this;
    }

    /**
     * Sets the interval count for this scale
     * @param intervalCount The interval count hint
     * @return This scale, for chaining
     */
    public ColorScale setIntervalCount(int intervalCount) {
        this.intervalCount = intervalCount;
        return this;
    }

    /**
     * Reverses this scale
     * @return This scale, for chaining
     */
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

    /**
     * draws an outline for the legend texts
     * @param g The graphics to draw on
     * @param txt The text to draw the outline
     * @param x Text x
     * @param y Text y
     * @param color The color of the text
     */
    private void drawOutline(final Graphics2D g, final String txt, final int x, final int y, final Color color) {
        if (ColorHelper.calculateContrastRatio(color, LEGEND_TEXT_OUTLINE_DARK) >=
            ColorHelper.calculateContrastRatio(color, LEGEND_TEXT_OUTLINE_BRIGHT)) {
            g.setColor(LEGEND_TEXT_OUTLINE_DARK);
        } else {
            g.setColor(LEGEND_TEXT_OUTLINE_BRIGHT);
        }

        g.drawString(txt, x -1, y -1);
        g.drawString(txt, x +1, y -1);
        g.drawString(txt, x -1, y +1);
        g.drawString(txt, x +1, y +1);
        g.setColor(color);
    }

    /**
     * Draws a color bar representing this scale on the given graphics
     * @param g The graphics to draw on
     * @param x Rect x
     * @param y Rect y
     * @param w Rect width
     * @param h Rect height
     * @param valueScale The scale factor of the values
     */
    public void drawColorBar(final Graphics2D g, final int x, final int y, final int w, final int h, final double valueScale) {
        final int n = colors.length;

        final FontMetrics fm = calculateFontMetrics(g);

        g.setColor(LEGEND_BACKGROUND);

        // color bar texts width & height
        final int fw;
        final int fh = fm.getHeight() / 2;

        // calculates the width of the color bar texts
        if (colorBarTitles != null && colorBarTitles.length > 0) {
            fw = Arrays.stream(colorBarTitles).mapToInt(fm::stringWidth).max().orElse(50);
        } else {
            fw = fm.stringWidth(
                    String.valueOf(Math.max((int) Math.abs(max * valueScale), (int) Math.abs(min * valueScale))))
                    + fm.stringWidth("0.123");
        }

        // background rectangle
        final int[] t = drawBackgroundRectangle(g, x, y, w, h, fw, fh, fm.stringWidth(title));
        final int xRect = t[0];
        final int rectWidth = t[1];
        final int xText = t[2];
        final int titleWidth = t[3];

        // colorbar
        for (int i = 0; i < n; i++) {
            g.setColor(colors[i]);
            if (w < h) {
                double factor = n == 6 ? 1.2 : 1.07 + (0.045 * Math.log(n));
                if (n < 200) {
                    g.fillRect(xText + fw + PADDING / 3, y - PADDING / 2 + i * (int) ((double) h / n * factor),
                            w, (int) ((double) h / n * factor));
                } else {
                    g.fillRect(xText + fw + PADDING / 3, y - PADDING / 2 + i * h / (int) (n * 0.875), w, (h / n + 1));
                }
            } else {
                g.fillRect(xText + fw + 7 + i * w / n, y, w / n, h + 1);
            }
        }

        // legend title
        g.setColor(LEGEND_TITLE);
        g.drawString(title, xRect + rectWidth / 2 - titleWidth / 2, y - fh * 3 / 2 - 10);

        // legend texts
        drawLegend(g, y, w, h, valueScale, fh, fw, xText);

        g.setColor(noDataColor);
    }

    /**
     * Draws a color bar representing the time scale on the given graphics
     * @param g The graphics to draw on
     * @param x Rect x
     * @param y Rect y
     * @param w Color bar width
     * @param h Color bar height
     * @param minVal start time of the track
     * @param maxVal end time of the track
     */
    public void drawColorBarTime(final Graphics2D g, final int x, final int y, final int w, final int h,
                                 final double minVal, final double maxVal) {
        final int n = colors.length;

        final FontMetrics fm = calculateFontMetrics(g);

        g.setColor(LEGEND_BACKGROUND);

        final int padding = PADDING;

        // color bar texts width & height
        final int fw;
        final int fh = fm.getHeight() / 2;

        // calculates the width of the colorbar texts
        if (maxVal - minVal > 86400) {
            fw = fm.stringWidth(DAY_TIME_FORMAT);
        } else {
            fw = fm.stringWidth(TIME_FORMAT);
        }

        // background rectangle
        final int[] t = drawBackgroundRectangle(g, x, y, w, h, fw, fh, fm.stringWidth(title));
        final int xRect = t[0];
        final int rectWidth = t[1];
        final int xText = t[2];
        final int titleWidth = t[3];

        // colorbar
        for (int i = 0; i < n; i++) {
            g.setColor(colors[i]);
            if (w < h) {
                g.fillRect(xText + fw + padding / 3, y - padding / 2 + i * h / (int) (n * 0.875), w, (h / n + 1));
            } else {
                g.fillRect(xText + fw + padding / 3 + i * w / n, y, w / n + 1, h);
            }
        }

        // legend title
        g.setColor(LEGEND_TITLE);
        g.drawString(title, xRect + rectWidth / 2 - titleWidth / 2, y - fh * 3 / 2 - padding / 2);

        // legend texts
        drawTimeLegend(g, y, x, h, minVal, maxVal, fh, fw, xText);

        g.setColor(noDataColor);
    }

    private static FontMetrics calculateFontMetrics(final Graphics2D g) {
        final Font newFont = UIManager.getFont("PopupMenu.font");
        g.setFont(newFont);
        return g.getFontMetrics();
    }

    /**
     * Draw the background rectangle
     * @param g The graphics to draw on
     * @param x Rect x
     * @param y Rect y
     * @param w Color bar width
     * @param h Color bar height
     * @param fw The font width
     * @param fh The font height
     * @param titleWidth The width of the title
     * @return an @{code int[]} of [xRect, rectWidth, xText, titleWidth] TODO investigate using records in Java 17
     */
    private int[] drawBackgroundRectangle(final Graphics2D g, final int x, final int y,
                                          final int w, final int h, final int fw, final int fh,
                                          int titleWidth) {
        final int xRect;
        final int rectWidth;
        final int xText;
        final int arcWidth = 20;
        final int arcHeight = 20;
        if (fw + w > titleWidth) {
            rectWidth = w + fw + PADDING * 2;
            xRect = x - rectWidth;
            xText = xRect + (int) (PADDING / 1.2);
            g.fillRoundRect(xRect, (fh * 3 / 2), rectWidth, h + y - (fh * 3 / 2) + (int) (PADDING / 1.5), arcWidth, arcHeight);
        } else {
            if (titleWidth >= 120) {
                titleWidth = 120;
            }
            rectWidth = w + titleWidth + PADDING + PADDING / 2;
            xRect = x - rectWidth;
            xText = xRect + PADDING / 2 + rectWidth / 2 - fw;
            g.fillRoundRect(xRect, (fh * 3 / 2), rectWidth, h + y - (fh * 3 / 2) + (int) (PADDING / 1.5), arcWidth, arcHeight);
        }
        return new int[] {xRect, rectWidth, xText, titleWidth};
    }

    /**
     * Draws the legend for the color bar representing the time scale on the given graphics
     * @param g The graphics to draw on
     * @param y Rect y
     * @param w Color bar width
     * @param h Color bar height
     * @param fw The font width
     * @param fh The font height
     * @param valueScale The scale factor of the values
     * @param xText The location to start drawing the text (x-axis)
     */
    private void drawLegend(final Graphics2D g, final int y, final int w, final int h, final double valueScale,
                            final int fh, final int fw, final int xText) {
        for (int i = 0; i <= intervalCount; i++) {
            final String txt;
            final Color color = colors[(int) (1.0 * i * colors.length / intervalCount - 1e-10)];
            g.setColor(color);

            if (colorBarTitles != null && i < colorBarTitles.length) {
                txt = colorBarTitles[i];
            } else {
                final double val = min+i*(max-min)/intervalCount;
                txt = String.format("%.3f", val*valueScale);
            }
            drawLegendText(g, y, w, h, fh, fw, xText, i, color, txt);
        }
    }

    /**
     * Draws the legend for the color bar representing the time scale on the given graphics
     * @param g The graphics to draw on
     * @param y Rect y
     * @param w Color bar width
     * @param h Color bar height
     * @param minVal start time of the track
     * @param maxVal end time of the track
     * @param fw The font width
     * @param fh The font height
     * @param xText The location to start drawing the text (x-axis)
     */
    private void drawTimeLegend(final Graphics2D g, final int y, final int w, final int h,
                                final double minVal, final double maxVal,
                                final int fh, final int fw, final int xText) {
        for (int i = 0; i <= intervalCount; i++) {
            final String txt;
            final Color color = colors[(int) (1.0 * i * colors.length / intervalCount - 1e-10)];
            g.setColor(color);

            if (colorBarTitles != null && i < colorBarTitles.length) {
                txt = colorBarTitles[i];
            } else {
                final double val = minVal + i * (maxVal - minVal) / intervalCount;
                final long longval = (long) val;

                final Date date = new Date(longval * 1000L);
                final Instant dateInst = date.toInstant();

                final ZoneId gmt = ZoneId.of("GMT");
                final ZonedDateTime zonedDateTime = dateInst.atZone(gmt);

                String formatted;

                if (maxVal-minVal > 86400) {
                    final DateTimeFormatter day = DateTimeFormatter.ofPattern(DAY_TIME_FORMAT);
                    formatted = zonedDateTime.format(day);
                } else {
                    final DateTimeFormatter time = DateTimeFormatter.ofPattern(TIME_FORMAT);
                    formatted = zonedDateTime.format(time);
                }

                txt = formatted;
            }
            drawLegendText(g, y, w, h, fh, fw, xText, i, color, txt);
        }
    }

    /**
     * Draws the legend for the color bar representing the time scale on the given graphics
     * @param g The graphics to draw on
     * @param y Rect y
     * @param w Color bar width
     * @param h Color bar height
     * @param fw The font width
     * @param fh The font height
     * @param xText The location to start drawing the text (x-axis)
     * @param color The color of the text to draw
     * @param txt The text string to draw
     * @param i The index of the legend (so we can calculate the y location)
     */
    private void drawLegendText(Graphics2D g, int y, int w, int h, int fh, int fw, int xText,
                                int i, Color color, String txt) {

        if (intervalCount == 0) {
            drawOutline(g, txt, xText, y + h / 2 + fh / 2, color);
            g.drawString(txt, xText, y + h / 2 + fh / 2);
        } else if (w < h) {
            drawOutline(g, txt, xText, y + i * h / intervalCount + fh / 2, color);
            g.drawString(txt, xText, y + i * h / intervalCount + fh / 2);
        } else {
            final int xLoc = xText + i * w / intervalCount - fw / 2 - (int) (PADDING / 1.3);
            final int yLoc = y + fh - 5;
            drawOutline(g, txt, xLoc, yLoc, color);
            g.drawString(txt, xLoc, yLoc);
        }
    }
}
