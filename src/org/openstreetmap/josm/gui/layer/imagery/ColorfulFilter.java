// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.util.Optional;

import org.openstreetmap.josm.tools.Logging;

/**
 * Colorful filter.
 * @since 11914 (extracted from ColorfulImageProcessor)
 */
public class ColorfulFilter implements BufferedImageOp {
    private final double colorfulness;

    /**
     * Create a new colorful filter.
     * @param colorfulness The colorfulness as defined in the {@link ColorfulImageProcessor} class.
     */
    ColorfulFilter(double colorfulness) {
        this.colorfulness = colorfulness;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        if (src.getWidth() == 0 || src.getHeight() == 0 || src.getType() == BufferedImage.TYPE_CUSTOM) {
            return src;
        }

        BufferedImage dest = Optional.ofNullable(dst).orElseGet(() -> createCompatibleDestImage(src, null));
        DataBuffer srcBuffer = src.getRaster().getDataBuffer();
        DataBuffer destBuffer = dest.getRaster().getDataBuffer();
        if (!(srcBuffer instanceof DataBufferByte) || !(destBuffer instanceof DataBufferByte)) {
            Logging.trace("Cannot apply color filter: Images do not use DataBufferByte.");
            return src;
        }

        int type = src.getType();
        if (type != dest.getType()) {
            Logging.trace("Cannot apply color filter: Src / Dest differ in type (" + type + '/' + dest.getType() + ')');
            return src;
        }
        int redOffset;
        int greenOffset;
        int blueOffset;
        int alphaOffset = 0;
        switch (type) {
        case BufferedImage.TYPE_3BYTE_BGR:
            blueOffset = 0;
            greenOffset = 1;
            redOffset = 2;
            break;
        case BufferedImage.TYPE_4BYTE_ABGR:
        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            blueOffset = 1;
            greenOffset = 2;
            redOffset = 3;
            break;
        case BufferedImage.TYPE_INT_ARGB:
        case BufferedImage.TYPE_INT_ARGB_PRE:
            redOffset = 0;
            greenOffset = 1;
            blueOffset = 2;
            alphaOffset = 3;
            break;
        default:
            Logging.trace("Cannot apply color filter: Source image is of wrong type (" + type + ").");
            return src;
        }
        doFilter((DataBufferByte) srcBuffer, (DataBufferByte) destBuffer, redOffset, greenOffset, blueOffset,
                alphaOffset, src.getAlphaRaster() != null);
        return dest;
    }

    private void doFilter(DataBufferByte src, DataBufferByte dest, int redOffset, int greenOffset, int blueOffset,
            int alphaOffset, boolean hasAlpha) {
        byte[] srcPixels = src.getData();
        byte[] destPixels = dest.getData();
        if (srcPixels.length != destPixels.length) {
            Logging.trace("Cannot apply color filter: Source/Dest lengths differ.");
            return;
        }
        int entries = hasAlpha ? 4 : 3;
        for (int i = 0; i < srcPixels.length; i += entries) {
            int r = srcPixels[i + redOffset] & 0xff;
            int g = srcPixels[i + greenOffset] & 0xff;
            int b = srcPixels[i + blueOffset] & 0xff;
            double luminosity = r * .21d + g * .72d + b * .07d;
            destPixels[i + redOffset] = mix(r, luminosity);
            destPixels[i + greenOffset] = mix(g, luminosity);
            destPixels[i + blueOffset] = mix(b, luminosity);
            if (hasAlpha) {
                destPixels[i + alphaOffset] = srcPixels[i + alphaOffset];
            }
        }
    }

    private byte mix(int color, double luminosity) {
        int val = (int) (colorfulness * color + (1 - colorfulness) * luminosity);
        if (val < 0) {
            return 0;
        } else if (val > 0xff) {
            return (byte) 0xff;
        } else {
            return (byte) val;
        }
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(src.getWidth(), src.getHeight());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return (Point2D) srcPt.clone();
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
