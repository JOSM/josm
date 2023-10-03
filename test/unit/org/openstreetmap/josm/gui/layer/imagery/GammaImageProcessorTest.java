// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link GammaImageProcessor} class.
 * @author Michael Zangl
 */
class GammaImageProcessorTest {
    /**
     * Test {@link GammaImageProcessor#setGamma(double)} and {@link GammaImageProcessor#getGamma()}
     */
    @Test
    void testSetGet() {
        GammaImageProcessor processor = new GammaImageProcessor();

        assertEquals(1, processor.getGamma(), 0.001);

        processor.setGamma(2);
        assertEquals(2, processor.getGamma(), 0.001);

        processor.setGamma(0);
        assertEquals(0, processor.getGamma(), 0.001);

        processor.setGamma(0.78);
        assertEquals(0.78, processor.getGamma(), 0.001);

        processor.setGamma(1);
        assertEquals(1, processor.getGamma(), 0.001);

        processor.setGamma(-1);
        assertEquals(-1, processor.getGamma(), 0.001);

        processor.setGamma(5);
        assertEquals(5, processor.getGamma(), 0.001);
    }

    /**
     * Test {@link GammaImageProcessor#toString()}
     */
    @Test
    void testToString() {
        GammaImageProcessor processor = new GammaImageProcessor();
        assertEquals("GammaImageProcessor [gamma=1.0]", processor.toString());
    }
}
