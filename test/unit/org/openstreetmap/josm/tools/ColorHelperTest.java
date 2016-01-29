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
}
