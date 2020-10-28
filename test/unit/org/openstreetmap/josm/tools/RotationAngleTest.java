// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RotationAngle} class.
 */
class RotationAngleTest {

    private static final double EPSILON = 1e-11;

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - nominal cases.
     */
    @Test
    void testParseCardinal() {
        assertEquals(Math.PI, RotationAngle.buildStaticRotation("south").getRotationAngle(null), EPSILON);
        assertEquals(Math.PI, RotationAngle.buildStaticRotation("s").getRotationAngle(null), EPSILON);
        assertEquals(Math.toRadians(315), RotationAngle.buildStaticRotation("northwest").getRotationAngle(null), EPSILON);
    }

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - wrong parameter.
     */
    @Test
    void testParseFail() {
        assertThrows(IllegalArgumentException.class, () -> RotationAngle.buildStaticRotation("bad"));
    }

    /**
     * Unit test of method {@link RotationAngle#buildStaticRotation} - null handling.
     */
    @Test
    void testParseNull() {
        assertThrows(NullPointerException.class, () -> RotationAngle.buildStaticRotation(null));
    }
}
