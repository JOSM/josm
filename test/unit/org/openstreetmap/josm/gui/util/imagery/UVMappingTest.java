// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util.imagery;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.geom.Point2D;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test class for {@link UVMapping}
 */
class UVMappingTest {
    private static final double DEFAULT_DELTA = 1e-5;

    static Stream<Arguments> testMapping() {
        return Stream.of(Arguments.of(0.5, 1, 0, 1, 0),
                Arguments.of(0.5, 0, 0, -1, 0),
                Arguments.of(0.25, 0.5, -1, 0, 0),
                Arguments.of(0.5, 0.5, 0, 0, 1),
                Arguments.of(0.75, 0.5, 1, 0, 0),
                Arguments.of(1, 0.5, 0, 0, -1),
                Arguments.of(0.125, 0.25, -0.5, -1 / Math.sqrt(2), -0.5),
                Arguments.of(0.625, 0.75, 0.5, 1 / Math.sqrt(2), 0.5)
                );
    }

    /**
     * Test that UV mapping is reversible for the sphere
     * @param px The x for the point
     * @param py The y for the point
     * @param x The x portion of the vector
     * @param y The y portion of the vector
     * @param z The z portion of the vector
     */
    @ParameterizedTest
    @MethodSource
    void testMapping(final double px, final double py, final double x, final double y, final double z) {
        // The mapping must be reversible
        assertAll(() -> assertPointEquals(new Point2D.Double(px, py), UVMapping.getTextureCoordinate(new Vector3D(x, y, z))),
                () -> assertVectorEquals(new Vector3D(x, y, z), UVMapping.getVector(px, py)));
    }

    @ParameterizedTest
    @ValueSource(floats = {0, 1, 1.1f, 0.9f})
    void testGetVectorEdgeCases(final float location) {
        if (location < 0 || location > 1) {
            assertAll(() -> assertThrows(IllegalArgumentException.class, () -> UVMapping.getVector(location, 0.5)),
                    () -> assertThrows(IllegalArgumentException.class, () -> UVMapping.getVector(0.5, location)));
        } else {
            assertAll(() -> assertDoesNotThrow(() -> UVMapping.getVector(location, 0.5)),
                    () -> assertDoesNotThrow(() -> UVMapping.getVector(0.5, location)));
        }
    }

    private static void assertVectorEquals(final Vector3D expected, final Vector3D actual) {
        final String message = String.format("Expected (%f %f %f), but was (%f %f %f)", expected.getX(),
                expected.getY(), expected.getZ(), actual.getX(), actual.getY(), actual.getZ());
        assertEquals(expected.getX(), actual.getX(), DEFAULT_DELTA, message);
        assertEquals(expected.getY(), actual.getY(), DEFAULT_DELTA, message);
        assertEquals(expected.getZ(), actual.getZ(), DEFAULT_DELTA, message);
    }

    private static void assertPointEquals(final Point2D expected, final Point2D actual) {
        final String message = String.format("Expected (%f, %f), but was (%f, %f)", expected.getX(), expected.getY(),
                actual.getX(), actual.getY());
        assertEquals(expected.getX(), actual.getX(), DEFAULT_DELTA, message);
        assertEquals(expected.getY(), actual.getY(), DEFAULT_DELTA, message);
    }
}
