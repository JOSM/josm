// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util.imagery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for {@link Vector3D}
 * @author Taylor Smock
 */
class Vector3DTest {

    static Stream<Arguments> vectorInformation() {
        return Stream.of(
            Arguments.of(0, 0, 0, 0),
            Arguments.of(1, 1, 1, Math.sqrt(3)),
            Arguments.of(-1, -1, -1, Math.sqrt(3)),
            Arguments.of(-2, 2, -2, Math.sqrt(12))
        );
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    void getX(final double x, final double y, final double z) {
        final Vector3D vector3D = new Vector3D(x, y, z);
        assertEquals(x, vector3D.getX());
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    void getY(final double x, final double y, final double z) {
        final Vector3D vector3D = new Vector3D(x, y, z);
        assertEquals(y, vector3D.getY());
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    void getZ(final double x, final double y, final double z) {
        final Vector3D vector3D = new Vector3D(x, y, z);
        assertEquals(z, vector3D.getZ());
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    void getRadialDistance(final double x, final double y, final double z, final double radialDistance) {
        final Vector3D vector3D = new Vector3D(x, y, z);
        assertEquals(radialDistance, vector3D.getRadialDistance());
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    @Disabled("Angle calculations may be corrected")
    void getPolarAngle() {
        fail("Not yet implemented");
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    @Disabled("Angle calculations may be corrected")
    void getAzimuthalAngle() {
        fail("Not yet implemented");
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    void normalize(final double x, final double y, final double z) {
        final Vector3D vector3D = new Vector3D(x, y, z);
        final Vector3D normalizedVector = vector3D.normalize();
        assertAll(() -> assertEquals(vector3D.getRadialDistance() == 0 ? 0 : 1, normalizedVector.getRadialDistance()),
                () -> assertEquals(vector3D.getPolarAngle(), normalizedVector.getPolarAngle()),
                () -> assertEquals(vector3D.getAzimuthalAngle(), normalizedVector.getAzimuthalAngle()));
    }

    @ParameterizedTest
    @MethodSource("vectorInformation")
    @Disabled("Angle calculations may be corrected")
    void testToString() {
        fail("Not yet implemented");
    }
}
