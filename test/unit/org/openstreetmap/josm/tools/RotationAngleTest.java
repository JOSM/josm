// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link RotationAngle} class.
 */
public class RotationAngleTest {

    private static final double EPSILON = 1e-11;

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - nominal cases.
     */
    @Test
    public void testParseCardinal() {
        assertEquals(Math.PI, RotationAngle.buildStaticRotation("south").getRotationAngle(null), EPSILON);
        assertEquals(Math.PI, RotationAngle.buildStaticRotation("s").getRotationAngle(null), EPSILON);
        assertEquals(Math.toRadians(315), RotationAngle.buildStaticRotation("northwest").getRotationAngle(null), EPSILON);
    }

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - wrong parameter.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParseFail() {
        RotationAngle.buildStaticRotation("bad");
    }

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - null handling.
     */
    @Test(expected = NullPointerException.class)
    public void testParseNull() {
        RotationAngle.buildStaticRotation(null);
    }
}
