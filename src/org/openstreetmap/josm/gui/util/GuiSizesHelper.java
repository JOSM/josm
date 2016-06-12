// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;

/**
 * Support class to handle size information of Gui elements
 * This is needed, because display resolution may vary a lot and a common set
 * of sizes wont work for all users alike.
 * @since 10358
 */
final public class GuiSizesHelper {

    private GuiSizesHelper() {
        // Hide default constructor for utils classes
    }


    /** cache value for screen resolution */
    private static int screenDPI = -1;

    /** Request the screen resolution (cached)
     * @return screen resolution in DPI
     */
    private static int getScreenDPI() {
        if (screenDPI == -1) {
            synchronized (GuiHelper.class) {
                if (screenDPI == -1) {
                    try {
                        screenDPI = Toolkit.getDefaultToolkit().getScreenResolution();
                    } catch (HeadlessException e) {
                        screenDPI = 96;
                    }
                        
                }
            }
        }
        return screenDPI;
    }

    /**
     * Returns coefficient of monitor pixel density. All hardcoded sizes must be multiplied by this value.
     *
     * @return float value. 1 - means standard monitor, 2 and high - "retina" display.
     */
    public static float getPixelDensity() {
        int pixelPerInch = getScreenDPI();
        return (float) (pixelPerInch / 96.0);
    }

    /**
     * Check if a high DPI resolution is used
     * @return <code>true</code> for HIDPI screens
     */
    public static boolean isHiDPI() {
        return getPixelDensity() >= 2f;
    }

    /**
     * Returns a resolution adapted size
     * @param size Size value to adapt (base size is a low DPI screen)
     * @return adapted size (may be unmodified)
     */
    public static int getSizeDpiAdjusted(int size) {
        if (size <= 0) return size;
        int pixelPerInch = getScreenDPI();
        return size * pixelPerInch / 96;
    }

    /**
     * Returns a resolution adapted size
     * @param size Size value to adapt (base size is a low DPI screen)
     * @return adapted size (may be unmodified)
     */
    public static float getSizeDpiAdjusted(float size) {
        if (size <= 0f) return size;
        int pixelPerInch = getScreenDPI();
        return size * pixelPerInch / 96;
    }

    /**
     * Returns a resolution adapted size
     * @param size Size value to adapt (base size is a low DPI screen)
     * @return adapted size (may be unmodified)
     */
    public static double getSizeDpiAdjusted(double size) {
        if (size <= 0d) return size;
        int pixelPerInch = getScreenDPI();
        return size * pixelPerInch / 96;
    }

    /**
     * Returns a resolution adapted Dimension
     * @param dim Dimension value to adapt (base size is a low DPI screen)
     * @return adapted dimension (may be unmodified)
     */
    public static Dimension getDimensionDpiAdjusted(Dimension dim) {
        int pixelPerInch = getScreenDPI();
        int width = dim.width, height = dim.height;
        if (dim.width > 0) {
            width = dim.width * pixelPerInch / 96;
        }

        if (dim.height > 0) {
            height = dim.height * pixelPerInch / 96;
        }

        return new Dimension(width, height);
    }
}
