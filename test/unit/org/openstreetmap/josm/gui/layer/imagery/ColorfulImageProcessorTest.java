// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for the {@link ColorfulImageProcessor} class.
 * @author Michael Zangl
 */
public class ColorfulImageProcessorTest {

    private static final int TEST_IMAGE_SIZE = 5;

    private static final int[] PALETTE = {
            Color.BLACK.getRGB(),
            Color.WHITE.getRGB(),
            Color.GRAY.getRGB(),
            Color.GREEN.getRGB(),
            Color.RED.getRGB(),
            Color.BLUE.getRGB(),
            0xff908050,
            0xff908070,
            0xff908070,
            0xff908070,
            0xfff02080,
    };

    private static final IndexColorModel COLOR_MODEL = new IndexColorModel(8, PALETTE.length, PALETTE, 0, true, 255, DataBuffer.TYPE_BYTE);

    /**
     * No special rules
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link ColorfulImageProcessor#setColorfulness(double)} and {@link ColorfulImageProcessor#getColorfulness()}
     */
    @Test
    public void testSetGet() {
        ColorfulImageProcessor processor = new ColorfulImageProcessor();

        assertEquals(1, processor.getColorfulness(), 0.001);

        processor.setColorfulness(2);
        assertEquals(2, processor.getColorfulness(), 0.001);

        processor.setColorfulness(0);
        assertEquals(0, processor.getColorfulness(), 0.001);

        processor.setColorfulness(0.78);
        assertEquals(0.78, processor.getColorfulness(), 0.001);

        processor.setColorfulness(1);
        assertEquals(1, processor.getColorfulness(), 0.001);

        processor.setColorfulness(-1);
        assertEquals(0, processor.getColorfulness(), 0.001);

        processor.setColorfulness(5);
        assertEquals(5, processor.getColorfulness(), 0.001);
    }

    /**
     *
     */
    @Test
    public void testProcessing() {
        for (ConversionData data : new ConversionData[] {
                new ConversionData(Color.BLACK, 1.5, Color.BLACK),
                new ConversionData(Color.WHITE, 0.5, Color.WHITE),
                new ConversionData(Color.GRAY, 0, Color.GRAY),
                new ConversionData(Color.GREEN, 1, Color.GREEN),
                new ConversionData(Color.RED, 1, Color.RED),
                new ConversionData(Color.BLUE, 1, Color.BLUE),
                new ConversionData(0x908050, 0, 0x808080),
                new ConversionData(0x908070, 1, 0x908070),
                new ConversionData(0x908070, 2, 0x9c7c5c),
                new ConversionData(0x908070, 2, 0x9c7c5c),
                new ConversionData(0xf02080, 2, 0xff00ac),
        }) {
            for (int type : new int[] {
                    BufferedImage.TYPE_3BYTE_BGR,
                    BufferedImage.TYPE_4BYTE_ABGR,
                    BufferedImage.TYPE_4BYTE_ABGR_PRE,
                    BufferedImage.TYPE_BYTE_INDEXED }) {
                assertTrue(runProcessing(data, type));
            }
        }
    }

    private boolean runProcessing(ConversionData data, int type) {
        BufferedImage image = createImage(data.getOldColor(), type);

        ColorfulImageProcessor processor = new ColorfulImageProcessor();
        processor.setColorfulness(data.getFactor());
        image = processor.process(image);

        for (int x = 0; x < TEST_IMAGE_SIZE; x++) {
            for (int y = 0; y < TEST_IMAGE_SIZE; y++) {
                Color color = new Color(image.getRGB(x, y));
                assertEquals(data + ":" + type + ": red", data.getExpectedColor().getRed(), color.getRed(), 1.05);
                assertEquals(data + ":" + type + ": green", data.getExpectedColor().getGreen(), color.getGreen(), 1.05);
                assertEquals(data + ":" + type + ": blue", data.getExpectedColor().getBlue(), color.getBlue(), 1.05);
            }
        }
        return true;
    }

    private BufferedImage createImage(Color color, int type) {
        BufferedImage image = type == BufferedImage.TYPE_BYTE_INDEXED
                ? new BufferedImage(TEST_IMAGE_SIZE, TEST_IMAGE_SIZE, type, COLOR_MODEL)
                : new BufferedImage(TEST_IMAGE_SIZE, TEST_IMAGE_SIZE, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, TEST_IMAGE_SIZE, TEST_IMAGE_SIZE);
        assertEquals(color.getRGB(), image.getRGB(1, 1));
        return image;
    }

    private static class ConversionData {
        private final Color oldColor;
        private final double factor;
        private final Color expectedColor;

        ConversionData(Color oldColor, double factor, Color expectedColor) {
            super();
            this.oldColor = oldColor;
            this.factor = factor;
            this.expectedColor = expectedColor;
        }

        ConversionData(int oldColor, double factor, int expectedColor) {
            this(new Color(oldColor), factor, new Color(expectedColor));
        }

        Color getOldColor() {
            return oldColor;
        }

        double getFactor() {
            return factor;
        }

        Color getExpectedColor() {
            return expectedColor;
        }

        @Override
        public String toString() {
            return "ConversionData [oldColor=" + oldColor + ", factor=" + factor + "]";
        }
    }

    /**
     * Test {@link ColorfulImageProcessor#toString()}
     */
    @Test
    public void testToString() {
        ColorfulImageProcessor processor = new ColorfulImageProcessor();
        assertEquals("ColorfulImageProcessor [colorfulness=1.0]", processor.toString());
    }
}
