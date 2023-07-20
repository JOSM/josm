// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for class {@link Shape}.
 */
class ShapeTest {

    /**
     * Tests string conversion
     */
    @Test
    void test() {
        Shape shape = new Shape();
        shape.addPoint("47.1", "11.1");
        shape.addPoint("47.2", "11.2");
        shape.addPoint("47.3", "11.3");
        String shapeString = "47.1 11.1 47.2 11.2 47.3 11.3";
        Shape fromString = new Shape(shapeString, " ");
        assertEquals(shape, fromString);
        assertEquals(shapeString, shape.encodeAsString(" "));
        assertEquals("47.1//11.1//47.2//11.2//47.3//11.3", shape.encodeAsString("//"));
        assertEquals("47.1,11.1,47.2,11.2,47.3,11.3;47.1,11.1,47.2,11.2,47.3,11.3", Shape.encodeAsString(Arrays.asList(shape, shape)));
    }

    /**
     * Check double edge cases
     * @param coordinate the coordinate to check
     */
    @ParameterizedTest
    @ValueSource(doubles = {
            // The double representation of 0.2575799 * 1e7 is 2575798.9999999995. Directly casting to int will round down.
            0.2575799,
            // Check that 2575798.0000000005 is rounded down
            0.2575798
    })
    void testDoubleEdgeCases(final double coordinate) {
        final Shape shape = new Shape();
        shape.addPoint(Double.toString(1), Double.toString(coordinate));
        shape.addPoint(Double.toString(coordinate), Double.toString(1));
        shape.addPoint(Double.toString(coordinate), Double.toString(coordinate));
        assertAll("Coordinates are not properly rounded on entry",
                () -> assertEquals(coordinate, shape.getPoints().get(0).getLon()),
                () -> assertEquals(coordinate, shape.getPoints().get(1).getLat()));
    }

    /**
     * Ensure that the hashcode is semantically the same as what it previously was
     */
    @ParameterizedTest
    @ValueSource(strings = {"47.1 11.1 47.2 11.2 47.3 11.3", "-47.1 -11.1 -47.2 -11.2 -47.3 -11.3"})
    void testHashCode(String shapeString) {
        final Shape shape = new Shape(shapeString, " ");
        assertEquals(Objects.hash(shape.getPoints()), shape.hashCode(),
                "The hashcode for shape should be the same as that for the point list (specific coord list)");
        assertEquals(Objects.hash(new ArrayList<>(shape.getPoints())), shape.hashCode(),
                "The hashcode for shape should be the same as that for the point list (non-specific)");
    }

    @Test
    void testEqualsHashCodeContract() {
        EqualsVerifier.simple().forClass(Shape.class)
                .withNonnullFields("coords")
                .withPrefabValues(Polygon.class, new Polygon(), new Polygon(new int[] {1, 2, 3}, new int[]{4, 5, 6}, 3))
                .verify();
    }
}
