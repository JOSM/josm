package org.openstreetmap.josm.data;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;

import static org.junit.Assert.assertTrue;

public class BoundsTests {

    @Test
    public void crossingTests() {
        Bounds b1 = new Bounds(0, 170, 50, -170);
        assertTrue(b1.crosses180thMeridian());
        assertTrue(!b1.contains(new LatLon(-10, -180)));
        assertTrue(b1.contains(new LatLon(0, -180)));
        assertTrue(b1.contains(new LatLon(50, -180)));
        assertTrue(!b1.contains(new LatLon(60, -180)));
        assertTrue(!b1.contains(new LatLon(-10, 180)));
        assertTrue(b1.contains(new LatLon(0, 180)));
        assertTrue(b1.contains(new LatLon(50, 180)));
        assertTrue(!b1.contains(new LatLon(60, 180)));

        Bounds b2 = new Bounds(60, 170, 90, -170);
        assertTrue(!b1.intersects(b2));
        assertTrue(!b2.intersects(b1));

        Bounds b3 = new Bounds(25, 170, 90, -170);
        assertTrue(b1.intersects(b3));
        assertTrue(b3.intersects(b1));
        assertTrue(b2.intersects(b3));
        assertTrue(b3.intersects(b2));
        
        b3.extend(b1);
        assertTrue(b3.equals(new Bounds(0, 170, 90, -170)));
        assertTrue(b1.intersects(b3));
        assertTrue(b3.intersects(b1));
        assertTrue(b2.intersects(b3));
        assertTrue(b3.intersects(b2));
        
        b3.extend(new LatLon(0, 0));
        assertTrue(b3.equals(new Bounds(0, 0, 90, -170)));
    }
}
