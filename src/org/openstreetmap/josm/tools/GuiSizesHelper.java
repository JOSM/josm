// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Support class to handle size information of Gui elements
 * This is needed, because display resolution may vary a lot and a common set
 * of sizes wont work for all users alike.
 * @since 12682 (moved from {@code gui.util} package)
 * @since 10358
 */
public final class GuiSizesHelper {

    private GuiSizesHelper() {
        // Hide default constructor for utils classes
    }

    /** cache value for screen resolution */
    private static float screenDPI = -1;

    /**
     * Request the screen resolution (cached)
     * @return screen resolution in DPI
     */
    private static float getScreenDPI() {
        if (screenDPI == -1) {
            synchronized (GuiSizesHelper.class) {
                if (screenDPI == -1) {
                    float scalePref = (float) Config.getPref().getDouble("gui.scale", 1.0);
                    if (scalePref != 0) {
                        screenDPI = 96f * scalePref;
                    } else {
                        if (!GraphicsEnvironment.isHeadless()) {
                            screenDPI = Toolkit.getDefaultToolkit().getScreenResolution();
                        } else {
                            screenDPI = 96;
                        }
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
        return getScreenDPI() / 96f;
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
        return Math.round(size * getScreenDPI() / 96);
    }

    /**
     * Returns a resolution adapted size
     * @param size Size value to adapt (base size is a low DPI screen)
     * @return adapted size (may be unmodified)
     */
    public static float getSizeDpiAdjusted(float size) {
        if (size <= 0f) return size;
        return size * getScreenDPI() / 96;
    }

    /**
     * Returns a resolution adapted size
     * @param size Size value to adapt (base size is a low DPI screen)
     * @return adapted size (may be unmodified)
     */
    public static double getSizeDpiAdjusted(double size) {
        if (size <= 0d) return size;
        return size * getScreenDPI() / 96;
    }

    /**
     * Returns a resolution adapted Dimension
     * @param dim Dimension value to adapt (base size is a low DPI screen)
     * @return adapted dimension (may be unmodified)
     */
    public static Dimension getDimensionDpiAdjusted(Dimension dim) {
        float pixelPerInch = getScreenDPI();
        int width = dim.width;
        int height = dim.height;
        if (dim.width > 0) {
            width = Math.round(dim.width * pixelPerInch / 96);
        }

        if (dim.height > 0) {
            height = Math.round(dim.height * pixelPerInch / 96);
        }

        return new Dimension(width, height);
    }
}
