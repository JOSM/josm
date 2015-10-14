// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Unit tests of {@link Geometry} class.
 */
public class GeometryTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link Geometry#getLineLineIntersection} method.
     */
    @Test
    public void testLineLineIntersection() {
        EastNorth p1 = new EastNorth(-9477809.106349014, 1.5392960539974203E7);
        EastNorth p2 = new EastNorth(-9477813.789091509, 1.5392954297092048E7);
        EastNorth p3 = new EastNorth(-9477804.974058038, 1.539295490030348E7);
        EastNorth p4 = new EastNorth(-9477814.628697459, 1.5392962142181376E7);

        EastNorth intersectionPoint = Geometry.getLineLineIntersection(p1, p2, p3, p4);

        EastNorth d1 = p3.subtract(intersectionPoint);
        EastNorth d2 = p1.subtract(p2);
        Double crossProduct = d1.east()*d2.north() - d1.north()*d2.east();
        Double scalarProduct = d1.east()*d2.east() + d1.north()*d2.north();
        Double len1 = d1.length();
        Double len2 = d2.length();

        Double angle1 = Geometry.getCornerAngle(p1, p2, intersectionPoint);
        Double angle2 = Geometry.getCornerAngle(p3, p4, intersectionPoint);
        Assert.assertTrue("intersection point not on line, angle: " + angle1,
                Math.abs(angle1) < 1e-10);
        Assert.assertTrue("intersection point not on line, angle: " + angle2,
                Math.abs(angle1) < 1e-10);

        Assert.assertTrue("cross product != 1 : " + Math.abs(crossProduct/len1/len2),
                Math.abs(Math.abs(crossProduct/len1/len2) - 1) < 1e-10);
        Assert.assertTrue("scalar product != 0 : " + scalarProduct/len1/len2,
                Math.abs(scalarProduct/len1/len2) < 1e-10);
    }
}
