// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test the {@link EastNorth} class
 * @author Michael Zangl
 * @since 10915
 */
class EastNorthTest {

    /**
     * Test {@link EastNorth#interpolate(EastNorth, double)}
     */
    @Test
    void testInterpolate() {
        EastNorth en1 = new EastNorth(0, 0);
        EastNorth en2 = new EastNorth(30, 60);
        EastNorth en3 = new EastNorth(-70, -40);
        // east:
        assertEquals(15, en1.interpolate(en2, 0.5).east(), 1e-10);
        assertEquals(0, en1.interpolate(en2, 0).east(), 1e-10);
        assertEquals(30, en1.interpolate(en2, 1).east(), 1e-10);
        assertEquals(0, en3.interpolate(en2, .7).east(), 1e-10);
        // north
        assertEquals(30, en1.interpolate(en2, 0.5).north(), 1e-10);
        assertEquals(0, en1.interpolate(en2, 0).north(), 1e-10);
        assertEquals(60, en1.interpolate(en2, 1).north(), 1e-10);
        assertEquals(0, en3.interpolate(en2, .4).north(), 1e-10);
    }

    /**
     * Test {@link EastNorth#getCenter(EastNorth)}
     */
    @Test
    void testGetCenter() {
        EastNorth en1 = new EastNorth(0, 0);
        EastNorth en2 = new EastNorth(30, 60);
        EastNorth en3 = new EastNorth(-70, -40);

        assertEquals(15, en1.getCenter(en2).east(), 1e-10);
        assertEquals(15, en2.getCenter(en1).east(), 1e-10);
        assertEquals(-20, en3.getCenter(en2).east(), 1e-10);

        assertEquals(30, en1.getCenter(en2).north(), 1e-10);
        assertEquals(30, en2.getCenter(en1).north(), 1e-10);
        assertEquals(10, en3.getCenter(en2).north(), 1e-10);
    }
}
