// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.awt.Color;

import org.junit.Test;

/**
 * Unit tests for class {@link ColorHelper}.
 */
public class ColorHelperTest {

    /**
     * Unit test of method {@link ColorHelper#html2color}.
     */
    @Test
    public void testHtml2color() {
        assertNull(ColorHelper.html2color(""));
        assertNull(ColorHelper.html2color("xyz"));
        assertEquals(Color.CYAN, ColorHelper.html2color("0ff"));
        assertEquals(Color.CYAN, ColorHelper.html2color("#0ff"));
        assertEquals(Color.CYAN, ColorHelper.html2color("00ffff"));
        assertEquals(Color.CYAN, ColorHelper.html2color("#00ffff"));
        assertEquals(Color.CYAN, ColorHelper.html2color("#00FFFF"));
        assertEquals(new Color(0x12345678, true), ColorHelper.html2color("#34567812"));
    }

    /**
     * Unit test of method {@link ColorHelper#color2html}.
     */
    @Test
    public void testColor2html() {
        assertNull(ColorHelper.color2html(null));
        assertEquals("#FF0000", ColorHelper.color2html(Color.RED));
        assertEquals("#00FFFF", ColorHelper.color2html(Color.CYAN));
        assertEquals("#34567812", ColorHelper.color2html(new Color(0x12345678, true)));
        assertEquals("#34567812", ColorHelper.color2html(new Color(0x12345678, true), true));
        assertEquals("#345678", ColorHelper.color2html(new Color(0x12345678, true), false));
    }

    /**
     * Unit test of method {@link ColorHelper#getForegroundColor}.
     */
    @Test
    public void testGetForegroundColor() {
        assertNull(ColorHelper.getForegroundColor(null));
        assertEquals(Color.WHITE, ColorHelper.getForegroundColor(Color.BLACK));
        assertEquals(Color.WHITE, ColorHelper.getForegroundColor(Color.DARK_GRAY));
        assertEquals(Color.BLACK, ColorHelper.getForegroundColor(Color.LIGHT_GRAY));
        assertEquals(Color.BLACK, ColorHelper.getForegroundColor(Color.YELLOW));
        assertEquals(Color.BLACK, ColorHelper.getForegroundColor(Color.WHITE));
    }

    /**
     * Test of {@link ColorHelper#float2int}
     */
    @Test
    public void testColorFloat2int() {
        assertNull(ColorHelper.float2int(null));
        assertEquals(255, (int) ColorHelper.float2int(-1.0f));
        assertEquals(0, (int) ColorHelper.float2int(-0.0f));
        assertEquals(0, (int) ColorHelper.float2int(0.0f));
        assertEquals(64, (int) ColorHelper.float2int(0.25f));
        assertEquals(128, (int) ColorHelper.float2int(0.5f));
        assertEquals(255, (int) ColorHelper.float2int(1.0f));
        assertEquals(255, (int) ColorHelper.float2int(2.0f));
    }

    /**
     * Test of {@link ColorHelper#int2float}
     */
    @Test
    public void testColorInt2float() {
        assertNull(ColorHelper.int2float(null));
        assertEquals(1.0f, ColorHelper.int2float(-1), 1e-3);
        assertEquals(0.0f, ColorHelper.int2float(0), 1e-3);
        assertEquals(0.25f, ColorHelper.int2float(64), 1e-3);
        assertEquals(0.502f, ColorHelper.int2float(128), 1e-3);
        assertEquals(0.753f, ColorHelper.int2float(192), 1e-3);
        assertEquals(1.0f, ColorHelper.int2float(255), 1e-3);
        assertEquals(1.0f, ColorHelper.int2float(1024), 1e-3);
    }

    /**
     * Test of {@link ColorHelper#alphaMultiply}
     */
    @Test
    public void testAlphaMultiply() {
        final Color color = new Color(0x12345678, true);
        assertEquals(new Color(0x12345678, true), ColorHelper.alphaMultiply(color, 1f));
        assertEquals(new Color(0x24345678, true), ColorHelper.alphaMultiply(color, 2f));
    }

    /**
     * Test of {@link ColorHelper#complement}
     */
    @Test
    public void testComplement() {
        assertEquals(Color.cyan, ColorHelper.complement(Color.red));
        assertEquals(Color.red, ColorHelper.complement(Color.cyan));
        assertEquals(Color.magenta, ColorHelper.complement(Color.green));
        assertEquals(Color.green, ColorHelper.complement(Color.magenta));
        assertEquals(Color.yellow, ColorHelper.complement(Color.blue));
        assertEquals(Color.blue, ColorHelper.complement(Color.yellow));
    }
}
