// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SharpenImageProcessor} class.
 * @author Michael Zangl
 */
class SharpenImageProcessorTest {
    /**
     * Test {@link SharpenImageProcessor#setSharpenLevel(float)} and {@link SharpenImageProcessor#getSharpenLevel()}
     */
    @Test
    void testSetGet() {
        SharpenImageProcessor processor = new SharpenImageProcessor();

        assertEquals(1, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(2);
        assertEquals(2, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(0);
        assertEquals(0, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(0.78f);
        assertEquals(0.78, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(1);
        assertEquals(1, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(-1);
        assertEquals(0, processor.getSharpenLevel(), 0.001);

        processor.setSharpenLevel(5);
        assertEquals(5, processor.getSharpenLevel(), 0.001);
    }

    /**
     * Test {@link SharpenImageProcessor#toString()}
     */
    @Test
    void testToString() {
        SharpenImageProcessor processor = new SharpenImageProcessor();
        assertEquals("SharpenImageProcessor [sharpenLevel=1.0]", processor.toString());
    }
}
