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
        if (html.length() > 0 && html.charAt(0) == '#')
            html = html.substring(1);
        if (html.length() == 3) {
            return html2color(new String(
                    new char[]{html.charAt(0), html.charAt(0), html.charAt(1), html.charAt(1), html.charAt(2), html.charAt(2)}));
        }
        if (html.length() != 6 && html.length() != 8)
            return null;
        try {
            return new Color(
                    Integer.parseInt(html.substring(0,2),16),
                    Integer.parseInt(html.substring(2,4),16),
                    Integer.parseInt(html.substring(4,6),16),
                    (html.length() == 8 ? Integer.parseInt(html.substring(6,8),16) : 255));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String int2hex(int i) {
        String s = Integer.toHexString(i / 16) + Integer.toHexString(i % 16);
        return s.toUpperCase();
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
     * @param col The color to convert
     * @param withAlpha if {@code true} and alpha value &lt; 255, return 8-digit color code, else always 6-digit
     * @return the HTML color code (6 or 8 digit)
     * @since 6655
     */
    public static String color2html(Color col, boolean withAlpha) {
        if (col == null)
            return null;
        String code = "#"+int2hex(col.getRed())+int2hex(col.getGreen())+int2hex(col.getBlue());
        if (withAlpha) {
            int alpha = col.getAlpha();
            if (alpha < 255) {
                code += int2hex(alpha);
            }
        }
        return code;
    }
}
