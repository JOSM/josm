// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link Composite} implementing the common "blend modes" of image editors (multiply, burn, hard light,
 * difference, divide), which the standard {@link java.awt.AlphaComposite} does not provide.
 * <p>
 * The source alpha (including the coverage produced by antialiasing) controls how strongly the blended color
 * replaces the destination, following the "source over" rule of the W3C compositing model, so that a
 * translucent destination is handled correctly as well. It works on any color model, but is implemented per
 * pixel and thus only meant for drawing thin shapes such as grid lines. It must be used on
 * {@link java.awt.image.BufferedImage} backed graphics, since hardware accelerated pipelines do not support
 * custom composites.
 * @since 19640
 */
public final class BlendComposite implements Composite {

    /**
     * The supported blend modes.
     */
    public enum Mode {
        /** Normal alpha blending (like {@link java.awt.AlphaComposite#SrcOver}) */
        NORMAL,
        /** Multiplies source and destination: darkens, white is neutral */
        MULTIPLY,
        /** Color burn: darkens and increases contrast, white is neutral */
        BURN,
        /** Hard light: multiplies for dark sources, screens for light sources; strong contrast */
        HARD_LIGHT,
        /** Absolute difference between source and destination: always visible on any background */
        DIFFERENCE,
        /** Divides destination by source: lightens, white is neutral, dark sources give bright lines */
        DIVIDE
    }

    private static final Map<Mode, BlendComposite> INSTANCES = new EnumMap<>(Mode.class);

    private final Mode mode;

    private BlendComposite(Mode mode) {
        this.mode = mode;
    }

    /**
     * Returns the composite for the given mode.
     * @param mode the blend mode
     * @return the composite
     */
    public static synchronized BlendComposite getInstance(Mode mode) {
        return INSTANCES.computeIfAbsent(mode, BlendComposite::new);
    }

    /**
     * Returns the blend mode of this composite.
     * @return the blend mode
     */
    public Mode getMode() {
        return mode;
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new BlendContext(mode, srcColorModel, dstColorModel);
    }

    /**
     * Blends one color channel.
     * @param mode blend mode
     * @param s source channel value (0-255)
     * @param d destination channel value (0-255)
     * @return blended value (0-255)
     */
    static int blend(Mode mode, int s, int d) {
        switch (mode) {
        case MULTIPLY:
            return s * d / 255;
        case BURN:
            return s == 0 ? 0 : 255 - Math.min(255, (255 - d) * 255 / s);
        case HARD_LIGHT:
            return s < 128 ? 2 * s * d / 255 : 255 - (255 - d) * (510 - 2 * s) / 255;
        case DIFFERENCE:
            return Math.abs(s - d);
        case DIVIDE:
            return s == 0 ? 255 : Math.min(255, d * 255 / s);
        case NORMAL:
        default:
            return s;
        }
    }

    /**
     * Composes one pixel: blends the source color with the destination and combines the two with the
     * "source over" rule, so that both the source alpha (including the coverage produced by antialiasing)
     * and a translucent destination are handled correctly.
     * @param mode blend mode
     * @param s source color (ARGB, not premultiplied)
     * @param d destination color (ARGB, not premultiplied)
     * @return the resulting color (ARGB, not premultiplied)
     */
    static int composePixel(Mode mode, int s, int d) {
        int sa = s >>> 24;
        if (sa == 0) {
            return d;
        }
        int da = d >>> 24;
        if (da == 0xff) {
            // opaque destination (the map view): the result is the destination moved towards the blended color
            return 0xff000000
                    | (mix(blend(mode, (s >> 16) & 0xff, (d >> 16) & 0xff), (d >> 16) & 0xff, sa) << 16)
                    | (mix(blend(mode, (s >> 8) & 0xff, (d >> 8) & 0xff), (d >> 8) & 0xff, sa) << 8)
                    | mix(blend(mode, s & 0xff, d & 0xff), d & 0xff, sa);
        }
        int a = sa + da * (0xff - sa) / 0xff;
        if (a == 0) {
            return 0;
        }
        return (a << 24)
                | (composeChannel(mode, (s >> 16) & 0xff, (d >> 16) & 0xff, sa, da, a) << 16)
                | (composeChannel(mode, (s >> 8) & 0xff, (d >> 8) & 0xff, sa, da, a) << 8)
                | composeChannel(mode, s & 0xff, d & 0xff, sa, da, a);
    }

    /**
     * Composes one color channel of a translucent destination, see
     * <a href="https://www.w3.org/TR/compositing-1/#blending">the W3C compositing model</a>:
     * {@code co = as*(1-ab)*Cs + as*ab*B(Cb,Cs) + (1-as)*ab*Cb} and {@code Co = co/ao}.
     * @param mode blend mode
     * @param cs source channel value (0-255)
     * @param cb destination (backdrop) channel value (0-255)
     * @param sa source alpha (0-255)
     * @param da destination alpha (0-255)
     * @param a the resulting alpha (0-255), must not be 0
     * @return the resulting channel value (0-255)
     */
    private static int composeChannel(Mode mode, int cs, int cb, int sa, int da, int a) {
        int co = sa * (0xff - da) * cs + sa * da * blend(mode, cs, cb) + (0xff - sa) * da * cb;
        return co / (0xff * a);
    }

    /** linear interpolation between d (alpha 0) and s (alpha 255) */
    private static int mix(int s, int d, int alpha) {
        return d + (s - d) * alpha / 0xff;
    }

    private static final class BlendContext implements CompositeContext {
        private final Mode mode;
        private final ColorModel srcColorModel;
        private final ColorModel dstColorModel;

        BlendContext(Mode mode, ColorModel srcColorModel, ColorModel dstColorModel) {
            this.mode = mode;
            this.srcColorModel = srcColorModel;
            this.dstColorModel = dstColorModel;
        }

        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int w = Math.min(Math.min(src.getWidth(), dstIn.getWidth()), dstOut.getWidth());
            int h = Math.min(Math.min(src.getHeight(), dstIn.getHeight()), dstOut.getHeight());
            Object srcPixel = null;
            Object dstPixel = null;
            Object outPixel = null;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    srcPixel = src.getDataElements(x, y, srcPixel);
                    dstPixel = dstIn.getDataElements(x, y, dstPixel);
                    int result = composePixel(mode, srcColorModel.getRGB(srcPixel), dstColorModel.getRGB(dstPixel));
                    outPixel = dstColorModel.getDataElements(result, outPixel);
                    dstOut.setDataElements(x, y, outPixel);
                }
            }
        }

        @Override
        public void dispose() {
            // nothing to dispose
        }
    }
}
