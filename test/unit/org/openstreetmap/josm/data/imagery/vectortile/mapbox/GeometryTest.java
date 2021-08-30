// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link Geometry}
 * @author Taylor Smock
 * @since 17862
 */
class GeometryTest {
    /**
     * Create a command integer fairly easily
     * @param command The command type (see {@link Command})
     * @param parameters The parameters for the command
     * @return A command integer
     */
    private static CommandInteger createCommandInteger(int command, int... parameters) {
        CommandInteger commandInteger = new CommandInteger(command);
        if (parameters != null) {
            for (int parameter : parameters) {
                commandInteger.addParameter(parameter);
            }
        }
        return commandInteger;
    }

    /**
     * Check the current
     * @param pathIterator The path to check
     * @param expected The expected coords
     */
    private static void checkCurrentSegmentAndIncrement(PathIterator pathIterator, float... expected) {
        float[] coords = new float[6];
        int type = pathIterator.currentSegment(coords);
        pathIterator.next();
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], coords[i]);
        }
        if (Arrays.asList(PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO).contains(type)) {
            assertEquals(2, expected.length, "You should check both x and y coordinates");
        } else if (PathIterator.SEG_QUADTO == type) {
            assertEquals(4, expected.length, "You should check all x and y coordinates");
        } else if (PathIterator.SEG_CUBICTO == type) {
            assertEquals(6, expected.length, "You should check all x and y coordinates");
        } else if (PathIterator.SEG_CLOSE == type) {
            assertEquals(0, expected.length, "CloseTo has no expected coordinates to check");
        }
    }

    @Test
    void testBadGeometry() {
        IllegalArgumentException badPointException = assertThrows(IllegalArgumentException.class,
          () -> new Geometry(GeometryTypes.POINT, Collections.singletonList(createCommandInteger(1))));
        assertEquals("POINT with 0 arguments is not understood", badPointException.getMessage());
        IllegalArgumentException badLineException = assertThrows(IllegalArgumentException.class,
          () -> new Geometry(GeometryTypes.LINESTRING, Collections.singletonList(createCommandInteger(15))));
        assertEquals("LINESTRING with 0 arguments is not understood", badLineException.getMessage());
    }

    @Test
    void testPoint() {
        CommandInteger moveTo = createCommandInteger(9, 17, 34);
        Geometry geometry = new Geometry(GeometryTypes.POINT, Collections.singletonList(moveTo));
        assertEquals(1, geometry.getShapes().size());
        Ellipse2D shape = (Ellipse2D) geometry.getShapes().iterator().next();
        assertEquals(17, shape.getCenterX());
        assertEquals(34, shape.getCenterY());
    }

    @Test
    void testLine() {
        CommandInteger moveTo = createCommandInteger(9, 2, 2);
        CommandInteger lineTo = createCommandInteger(18, 0, 8, 8, 0);
        Geometry geometry = new Geometry(GeometryTypes.LINESTRING, Arrays.asList(moveTo, lineTo));
        assertEquals(1, geometry.getShapes().size());
        Path2D path = (Path2D) geometry.getShapes().iterator().next();
        PathIterator pathIterator = path.getPathIterator(null);
        checkCurrentSegmentAndIncrement(pathIterator, 2, 2);
        checkCurrentSegmentAndIncrement(pathIterator, 2, 10);
        checkCurrentSegmentAndIncrement(pathIterator, 10, 10);
        assertTrue(pathIterator.isDone());
    }

    @Test
    void testPolygon() {
        List<CommandInteger> commands = new ArrayList<>(3);
        commands.add(createCommandInteger(9, 3, 6));
        commands.add(createCommandInteger(18, 5, 6, 12, 22));
        commands.add(createCommandInteger(15));

        Geometry geometry = new Geometry(GeometryTypes.POLYGON, commands);
        assertEquals(1, geometry.getShapes().size());

        Area area = (Area) geometry.getShapes().iterator().next();
        PathIterator pathIterator = area.getPathIterator(null);
        checkCurrentSegmentAndIncrement(pathIterator, 3, 6);
        // This is somewhat unexpected, and may change based off of JVM implementations
        // But for whatever reason, Java flips the inner coordinates in this case.
        checkCurrentSegmentAndIncrement(pathIterator, 20, 34);
        checkCurrentSegmentAndIncrement(pathIterator, 8, 12);
        checkCurrentSegmentAndIncrement(pathIterator, 3, 6);
        checkCurrentSegmentAndIncrement(pathIterator);
        assertTrue(pathIterator.isDone());
    }

    @Test
    void testBadPolygon() {
        /*
         * "Linear rings MUST be geometric objects that have no anomalous geometric points,
         * such as self-intersection or self-tangency. The position of the cursor before
         * calling the ClosePath command of a linear ring SHALL NOT repeat the same position
         * as the first point in the linear ring as this would create a zero-length line
         * segment. A linear ring SHOULD NOT have an area calculated by the surveyor's
         * formula equal to zero, as this would signify a ring with anomalous geometric points."
         */
        List<CommandInteger> commands = new ArrayList<>(3);
        commands.add(createCommandInteger(9, 0, 0));
        commands.add(createCommandInteger(18, 0, 0));
        commands.add(createCommandInteger(15));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Geometry(GeometryTypes.POLYGON, commands));
        assertEquals("POLYGON cannot have zero area", exception.getMessage());
    }

    /**
     * This checks that the area is properly calculated
     */
    @Test
    void testNonRegression20971And21254() {
        assertEquals(15.0, Geometry.calculateSurveyorsArea(new int[]{1507, 1509, 1509}, new int[]{3029, 3018, 3033}));
        assertEquals(0.0, Geometry.calculateSurveyorsArea(new int[]{0, 0, 0}, new int[]{0, 0, 0}));
        assertEquals(0.0, Geometry.calculateSurveyorsArea(new int[2], new int[2]));
        assertThrows(IllegalArgumentException.class, () -> Geometry.calculateSurveyorsArea(new int[3], new int[4]));
    }

    @Test
    void testMultiPolygon() {
        List<CommandInteger> commands = new ArrayList<>(10);
        // Polygon 1
        commands.add(createCommandInteger(9, 0, 0));
        commands.add(createCommandInteger(26, 10, 0, 0, 10, -10, 0));
        commands.add(createCommandInteger(15));
        // Polygon 2 outer
        commands.add(createCommandInteger(9, 11, 1));
        commands.add(createCommandInteger(26, 9, 0, 0, 9, -9, 0));
        commands.add(createCommandInteger(15));
        // Polygon 2 inner
        commands.add(createCommandInteger(9, 2, -7));
        commands.add(createCommandInteger(26, 0, 4, 4, 0, 0, -4));
        commands.add(createCommandInteger(15));

        Geometry geometry = new Geometry(GeometryTypes.POLYGON, commands);
        assertEquals(1, geometry.getShapes().size());
        Area area = (Area) geometry.getShapes().iterator().next();
        assertFalse(area.isSingular());
        PathIterator pathIterator = area.getPathIterator(null);
        assertEquals(PathIterator.WIND_NON_ZERO, pathIterator.getWindingRule());
        assertTrue(area.contains(new Point2D.Float(5, 5)));
        assertTrue(area.contains(new Point2D.Float(12, 12)));
        assertFalse(area.contains(new Point2D.Float(15, 15)));
        assertFalse(area.contains(new Point2D.Float(10, 11)));
        assertFalse(area.contains(new Point2D.Float(-1, -1)));
    }
}
