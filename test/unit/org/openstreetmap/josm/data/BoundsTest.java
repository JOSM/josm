// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests for class {@link Bounds}.
 */
class BoundsTest {

    @Test
    void testCrossing() {
        Bounds b1 = new Bounds(0, 170, 50, -170);
        assertTrue(b1.crosses180thMeridian());
        assertFalse(b1.contains(new LatLon(-10, -180)));
        assertTrue(b1.contains(new LatLon(0, -180)));
        assertTrue(b1.contains(new LatLon(50, -180)));
        assertFalse(b1.contains(new LatLon(60, -180)));
        assertFalse(b1.contains(new LatLon(-10, 180)));
        assertTrue(b1.contains(new LatLon(0, 180)));
        assertTrue(b1.contains(new LatLon(50, 180)));
        assertFalse(b1.contains(new LatLon(60, 180)));

        Bounds b2 = new Bounds(60, 170, 90, -170);
        assertFalse(b1.intersects(b2));
        assertFalse(b2.intersects(b1));

        Bounds b3 = new Bounds(25, 170, 90, -170);
        assertTrue(b1.intersects(b3));
        assertTrue(b3.intersects(b1));
        assertTrue(b2.intersects(b3));
        assertTrue(b3.intersects(b2));

        b3.extend(b1);
        assertEquals(b3, new Bounds(0, 170, 90, -170));
        assertTrue(b1.intersects(b3));
        assertTrue(b3.intersects(b1));
        assertTrue(b2.intersects(b3));
        assertTrue(b3.intersects(b2));

        b3.extend(LatLon.ZERO);
        assertEquals(b3, new Bounds(0, 0, 90, -170));
    }

    private static void doTestConstructorNominal(Bounds b) {
        double eps = 1e-7;
        assertEquals(1d, b.getMinLat(), eps);
        assertEquals(2d, b.getMinLon(), eps);
        assertEquals(3d, b.getMaxLat(), eps);
        assertEquals(4d, b.getMaxLon(), eps);
    }

    private static void doTestConstructorPoint(Bounds b) {
        double eps = 1e-7;
        assertEquals(1d, b.getMinLat(), eps);
        assertEquals(2d, b.getMinLon(), eps);
        assertEquals(1d, b.getMaxLat(), eps);
        assertEquals(2d, b.getMaxLon(), eps);
    }

    /**
     * Unit tests for {@link Bounds#Bounds} - nominal cases.
     */
    @Test
    void testConstructorNominalCases() {
        doTestConstructorNominal(new Bounds(new LatLon(1d, 2d), new LatLon(3d, 4d)));
        doTestConstructorNominal(new Bounds(new LatLon(1d, 2d), new LatLon(3d, 4d), true));
        doTestConstructorNominal(new Bounds(1d, 2d, 3d, 4d));
        doTestConstructorNominal(new Bounds(1d, 2d, 3d, 4d, true));
        doTestConstructorNominal(new Bounds(new double[]{1d, 2d, 3d, 4d}));
        doTestConstructorNominal(new Bounds(new double[]{1d, 2d, 3d, 4d}, true));
        doTestConstructorNominal(new Bounds(new Bounds(1d, 2d, 3d, 4d)));
        doTestConstructorNominal(new Bounds(new Rectangle2D.Double(2d, 1d, 2d, 2d)));
    }

    /**
     * Unit tests for {@link Bounds#Bounds} - single point cases.
     */
    @Test
    void testConstructorSinglePointCases() {
        doTestConstructorPoint(new Bounds(new LatLon(1d, 2d)));
        doTestConstructorPoint(new Bounds(new LatLon(1d, 2d), true));
        doTestConstructorPoint(new Bounds(1d, 2d, true));
    }
}
