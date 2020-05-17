// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for class {@link Shape}.
 */
public class ShapeTest {

    /**
     * Tests string conversion
     */
    @Test
    public void test() {
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
}
