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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.ImageProcessor;

/**
 * Adds or removes the colorfulness of the image.
 *
 * @author Michael Zangl
 * @since 10547
 */
public class ColorfulImageProcessor implements ImageProcessor {
    private ColorfulFilter op;
    private double colorfulness = 1;

    /**
     * Gets the colorfulness value.
     * @return The value
     */
    public double getColorfulness() {
        return colorfulness;
    }

    /**
     * Sets the colorfulness value. Clamps it to 0+
     * @param colorfulness The value
     */
    public void setColorfulness(double colorfulness) {
        if (colorfulness < 0) {
            this.colorfulness = 0;
        } else {
            this.colorfulness = colorfulness;
        }

        if (this.colorfulness < .95 || this.colorfulness > 1.05) {
            op = new ColorfulFilter(this.colorfulness);
        } else {
            op = null;
        }
    }

    @Override
    public BufferedImage process(BufferedImage image) {
        if (op != null) {
            return op.filter(image, null);
        } else {
            return image;
        }
    }

    @Override
    public String toString() {
        return "ColorfulImageProcessor [colorfulness=" + colorfulness + ']';
    }

    static class ColorfulFilter implements BufferedImageOp {
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
            if (src.getWidth() == 0 || src.getHeight() == 0) {
                return src;
            }

            BufferedImage dest = dst;
            if (dest == null) {
                dest = createCompatibleDestImage(src, null);
            }
            DataBuffer srcBuffer = src.getRaster().getDataBuffer();
            DataBuffer destBuffer = dest.getRaster().getDataBuffer();
            if (!(srcBuffer instanceof DataBufferByte) || !(destBuffer instanceof DataBufferByte)) {
                Main.trace("Cannot apply color filter: Images do not use DataBufferByte.");
                return src;
            }

            int type = src.getType();
            if (type != dest.getType()) {
                Main.trace("Cannot apply color filter: Src / Dest differ in type (" + type + '/' + dest.getType() + ')');
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
                Main.trace("Cannot apply color filter: Source image is of wrong type (" + type + ").");
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
                Main.trace("Cannot apply color filter: Source/Dest lengths differ.");
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
}
