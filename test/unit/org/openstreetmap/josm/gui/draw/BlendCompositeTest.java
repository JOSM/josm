// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.draw.BlendComposite.Mode;

/**
 * Unit tests of {@link BlendComposite}.
 */
class BlendCompositeTest {

    private static int paint(int imageType, Color background, Color paint, Mode mode) {
        BufferedImage img = new BufferedImage(4, 4, imageType);
        Graphics2D g = img.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, 4, 4);
        g.setComposite(BlendComposite.getInstance(mode));
        g.setColor(paint);
        g.fillRect(1, 1, 2, 2);
        g.dispose();
        // the untouched pixel keeps the background
        assertEquals(background.getRGB() & 0xffffff, img.getRGB(0, 0) & 0xffffff);
        return img.getRGB(1, 1) & 0xffffff;
    }

    /**
     * Blend modes on an opaque source, on both an image without and with alpha channel.
     * @param imageType image type
     */
    @ParameterizedTest
    @ValueSource(ints = {BufferedImage.TYPE_3BYTE_BGR, BufferedImage.TYPE_INT_RGB, BufferedImage.TYPE_INT_ARGB})
    void testModes(int imageType) {
        Color bg = new Color(200, 100, 50);
        Color fg = new Color(128, 255, 0);
        assertEquals(0x80ff00, paint(imageType, bg, fg, Mode.NORMAL));
        assertEquals(new Color(200 * 128 / 255, 100, 0).getRGB() & 0xffffff, paint(imageType, bg, fg, Mode.MULTIPLY));
        assertEquals(new Color(146, 100, 0).getRGB() & 0xffffff, paint(imageType, bg, fg, Mode.BURN));
        assertEquals(new Color(201, 255, 0).getRGB() & 0xffffff, paint(imageType, bg, fg, Mode.HARD_LIGHT));
        assertEquals(new Color(72, 155, 50).getRGB() & 0xffffff, paint(imageType, bg, fg, Mode.DIFFERENCE));
        assertEquals(new Color(255, 100, 255).getRGB() & 0xffffff, paint(imageType, bg, fg, Mode.DIVIDE));
    }

    /**
     * A translucent source only partially applies the blended color.
     */
    @Test
    void testAlpha() {
        Color bg = new Color(200, 200, 200);
        // half transparent black, multiply: 200 -> 0 at full alpha -> 100 at half alpha
        int rgb = paint(BufferedImage.TYPE_INT_RGB, bg, new Color(0, 0, 0, 128), Mode.MULTIPLY);
        assertEquals(100, (rgb >> 16) & 0xff, 1);
        assertEquals(100, rgb & 0xff, 1);
        // fully transparent: nothing changes
        assertEquals(bg.getRGB() & 0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, new Color(0, 0, 0, 0), Mode.DIFFERENCE));
    }

    /**
     * White is neutral for multiply, burn and divide; black is neutral for difference
     */
    @Test
    void testNeutralColors() {
        Color bg = new Color(12, 34, 56);
        assertEquals(bg.getRGB() & 0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, Color.WHITE, Mode.MULTIPLY));
        assertEquals(bg.getRGB() & 0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, Color.WHITE, Mode.BURN));
        assertEquals(bg.getRGB() & 0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, Color.WHITE, Mode.DIVIDE));
        assertEquals(bg.getRGB() & 0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, Color.BLACK, Mode.DIFFERENCE));
        // black burns everything to black, white hard light gives white
        assertEquals(0, paint(BufferedImage.TYPE_INT_RGB, bg, Color.BLACK, Mode.BURN));
        assertEquals(0xffffff, paint(BufferedImage.TYPE_INT_RGB, bg, Color.WHITE, Mode.HARD_LIGHT));
    }

    /**
     * A translucent destination is composed with the "source over" rule: the result alpha is
     * {@code as + ab*(1-as)} and the blend only applies where the backdrop is actually present.
     */
    @Test
    void testTranslucentDestination() {
        // opaque source over a half transparent destination: the source wins, the result is opaque
        int argb = BlendComposite.composePixel(Mode.MULTIPLY, 0xff808080, 0x80ffffff);
        assertEquals(0xff, argb >>> 24);
        assertEquals(0x80, (argb >> 16) & 0xff, 1);

        // half transparent source over a fully transparent destination: the source shows unblended
        argb = BlendComposite.composePixel(Mode.MULTIPLY, 0x80123456, 0x00000000);
        assertEquals(0x80, argb >>> 24);
        assertEquals(0x12, (argb >> 16) & 0xff, 1);
        assertEquals(0x34, (argb >> 8) & 0xff, 1);
        assertEquals(0x56, argb & 0xff, 1);

        // half transparent source over a half transparent destination: alpha is 128 + 128*(1-128/255)
        argb = BlendComposite.composePixel(Mode.NORMAL, 0x80ffffff, 0x80000000);
        assertEquals(128 + 128 * (255 - 128) / 255, argb >>> 24);

        // a transparent source never changes the destination, whatever the mode
        for (Mode mode : Mode.values()) {
            assertEquals(0x8012ab34, BlendComposite.composePixel(mode, 0x00ffffff, 0x8012ab34), mode::toString);
        }

        // an opaque destination keeps its alpha and is only moved towards the blended color
        assertEquals(0xff000000, BlendComposite.composePixel(Mode.MULTIPLY, 0xff000000, 0xffffffff));
    }

    /**
     * Instances are shared per mode
     */
    @Test
    void testInstances() {
        assertSame(BlendComposite.getInstance(Mode.MULTIPLY), BlendComposite.getInstance(Mode.MULTIPLY));
        assertEquals(Mode.DIVIDE, BlendComposite.getInstance(Mode.DIVIDE).getMode());
    }
}
