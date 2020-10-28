// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link ColorScale}.
 */
class ColorScaleTest {

    /**
     * Test method for {@link ColorScale#createHSBScale(int)}.
     */
    @Test
    void testHSBScale() {
        final ColorScale scale = ColorScale.createHSBScale(256);
        assertEquals(new Color(255, 0, 0), scale.getColor(0));
        assertEquals(new Color(0, 255, 143), scale.getColor(128));
        assertEquals(new Color(255, 0, 229), scale.getColor(255));

        assertEquals(new Color(255, 0, 0), scale.getColor(-1000));
        assertEquals(new Color(255, 0, 229), scale.getColor(1000));

        assertNull(scale.getColor(Double.NaN));
        assertNull(scale.getColor(null));
        scale.setNoDataColor(new Color(12, 34, 56));
        assertEquals(new Color(12, 34, 56), scale.getNoDataColor());
        assertEquals(new Color(12, 34, 56), scale.getColor(Double.NaN));
        assertEquals(new Color(12, 34, 56), scale.getColor(null));
    }
}
