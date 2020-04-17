// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;

/**
 * Helper to convert from color to HTML string and back.
 */
public final class ColorHelper {

    private ColorHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * Returns the {@code Color} for the given HTML code.
     * @param html the color code
     * @return the color
     */
    public static Color html2color(String html) {
        if (!html.isEmpty() && html.charAt(0) == '#')
            html = html.substring(1);
        if (html.length() == 3) {
            html = new String(new char[]{
                    html.charAt(0), html.charAt(0),
                    html.charAt(1), html.charAt(1),
                    html.charAt(2), html.charAt(2)});
        }
        if (html.length() != 6 && html.length() != 8)
            return null;
        try {
            return new Color(
                    Integer.parseInt(html.substring(0, 2), 16),
                    Integer.parseInt(html.substring(2, 4), 16),
                    Integer.parseInt(html.substring(4, 6), 16),
                    html.length() == 8 ? Integer.parseInt(html.substring(6, 8), 16) : 255);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the HTML color code (6 or 8 digit).
     * @param col The color to convert
     * @return the HTML color code (6 or 8 digit)
     */
    public static String color2html(Color col) {
        return color2html(col, true);
    }

    /**
     * Returns the HTML color code (6 or 8 digit).
     * @param color The color to convert
     * @param withAlpha if {@code true} and alpha value &lt; 255, return 8-digit color code, else always 6-digit
     * @return the HTML color code (6 or 8 digit)
     * @since 6655
     */
    public static String color2html(Color color, boolean withAlpha) {
        if (color == null)
            return null;
        int alpha = color.getAlpha();
        return withAlpha && alpha != 255
                ? String.format("#%06X%02X", color.getRGB() & 0x00ffffff, alpha)
                : String.format("#%06X", color.getRGB() & 0x00ffffff);
    }

    /**
     * Determines the correct foreground color (black or white) to use for the given background,
     * so the text will be readable.
     * @param bg background color
     * @return {@code Color#BLACK} or {@code Color#WHITE}
     * @since 9223
     */
    public static Color getForegroundColor(Color bg) {
        // http://stackoverflow.com/a/3943023/2257172
        return bg == null ? null :
              (bg.getRed()*0.299 + bg.getGreen()*0.587 + bg.getBlue()*0.114) > 186 ?
                  Color.BLACK : Color.WHITE;
    }

    /**
     * convert float range 0 &lt;= x &lt;= 1 to integer range 0..255
     * when dealing with colors and color alpha value
     * @param val float value between 0 and 1
     * @return null if val is null, the corresponding int if val is in the
     *         range 0...1. If val is outside that range, return 255
     */
    public static Integer float2int(Float val) {
        if (val == null)
            return null;
        if (val < 0 || val > 1)
            return 255;
        return (int) (255f * val + 0.5f);
    }

    /**
     * convert integer range 0..255 to float range 0 &lt;= x &lt;= 1
     * when dealing with colors and color alpha value
     * @param val integer value
     * @return corresponding float value in range 0 &lt;= x &lt;= 1
     */
    public static Float int2float(Integer val) {
        if (val == null)
            return null;
        if (val < 0 || val > 255)
            return 1f;
        return ((float) val) / 255f;
    }

    /**
     * Multiply the alpha value of the given color with the factor. The alpha value is clamped to 0..255
     * @param color The color
     * @param alphaFactor The factor to multiply alpha with.
     * @return The new color.
     * @since 11692
     */
    public static Color alphaMultiply(Color color, float alphaFactor) {
        int alpha = float2int(int2float(color.getAlpha()) * alphaFactor);
        alpha = Utils.clamp(alpha, 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Returns the complementary color of {@code clr}.
     * @param clr the color to complement
     * @return the complementary color of {@code clr}
     */
    public static Color complement(Color clr) {
        return new Color(255 - clr.getRed(), 255 - clr.getGreen(), 255 - clr.getBlue(), clr.getAlpha());
    }
}
