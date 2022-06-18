// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A class to generate geometry for a vector tile
 * @author Taylor Smock
 * @since 17862
 */
public class Geometry {
    final Collection<Shape> shapes;

    /**
     * Create a {@link Geometry} for a {@link Feature}
     * @param geometryType The type of geometry
     * @param commands The commands used to create the geometry
     * @throws IllegalArgumentException if arguments are not understood or if the shoelace formula returns 0 for a polygon ring.
     */
    public Geometry(GeometryTypes geometryType, List<CommandInteger> commands) {
        if (geometryType == GeometryTypes.POINT) {
            // This gets rid of most of the expensive array copies from ArrayList#grow
            shapes = new ArrayList<>(commands.size());
            initializePoints(geometryType, commands);
        } else if (geometryType == GeometryTypes.LINESTRING || geometryType == GeometryTypes.POLYGON) {
            // This gets rid of most of the expensive array copies from ArrayList#grow
            shapes = new ArrayList<>(1);
            initializeWayGeometry(geometryType, commands);
        } else {
            shapes = Collections.emptyList();
        }
    }

    /**
     * Initialize point geometry
     * @param geometryType The geometry type (used for logging)
     * @param commands The commands to use to create the geometry
     */
    private void initializePoints(GeometryTypes geometryType, List<CommandInteger> commands) {
        for (CommandInteger command : commands) {
            final short[] operations = command.getOperations();
            // Each MoveTo command is a new point
            if (command.getType() == Command.MoveTo && operations.length % 2 == 0 && operations.length > 0) {
                for (int i = 0; i < operations.length / 2; i++) {
                    // Just using Ellipse2D since it extends Shape
                    shapes.add(new Ellipse2D.Float(operations[2 * i], operations[2 * i + 1], 0, 0));
                }
            } else {
                throw new IllegalArgumentException(tr("{0} with {1} arguments is not understood", geometryType, operations.length));
            }
        }
    }

    /**
     * Initialize way geometry
     * @param geometryType The geometry type
     * @param commands The commands to use to create the geometry
     */
    private void initializeWayGeometry(GeometryTypes geometryType, List<CommandInteger> commands) {
        Path2D.Float line = null;
        Area area = null;
        // MVT uses delta encoding. Each feature starts at (0, 0).
        int x = 0;
        int y = 0;
        // Area is used to determine the inner/outer of a polygon
        final int maxArraySize = commands.stream().filter(command -> command.getType() != Command.ClosePath)
                .mapToInt(command -> command.getOperations().length).sum();
        final List<Integer> xArray = new ArrayList<>(maxArraySize);
        final List<Integer> yArray = new ArrayList<>(maxArraySize);
        for (CommandInteger command : commands) {
            final short[] operations = command.getOperations();
            // Technically, there is no reason why there can be multiple MoveTo operations in one command, but that is undefined behavior
            if (command.getType() == Command.MoveTo && operations.length == 2) {
                x += operations[0];
                y += operations[1];
                // Avoid fairly expensive Arrays.copyOf calls
                line = new Path2D.Float(Path2D.WIND_NON_ZERO, commands.size());
                line.moveTo(x, y);
                xArray.add(x);
                yArray.add(y);
                shapes.add(line);
            } else if (command.getType() == Command.LineTo && operations.length % 2 == 0 && line != null) {
                for (int i = 0; i < operations.length / 2; i++) {
                    x += operations[2 * i];
                    y += operations[2 * i + 1];
                    xArray.add(x);
                    yArray.add(y);
                    line.lineTo(x, y);
                }
                // ClosePath should only be used with Polygon geometry
            } else if (geometryType == GeometryTypes.POLYGON && command.getType() == Command.ClosePath && line != null) {
                shapes.remove(line);
                // new Area() closes the line if it isn't already closed
                if (area == null) {
                    area = new Area();
                    shapes.add(area);
                }

                final double areaAreaSq = calculateSurveyorsArea(xArray.stream().mapToInt(i -> i).toArray(),
                        yArray.stream().mapToInt(i -> i).toArray());
                Area nArea = new Area(line);
                // SonarLint thinks that this is never > 0. It can be.
                if (areaAreaSq > 0) {
                    area.add(nArea);
                } else if (areaAreaSq < 0) {
                    area.exclusiveOr(nArea);
                } else {
                    throw new IllegalArgumentException(tr("{0} cannot have zero area", geometryType));
                }
                xArray.clear();
                yArray.clear();
            } else {
                throw new IllegalArgumentException(tr("{0} with {1} arguments is not understood", geometryType, operations.length));
            }
        }
    }

    /**
     * This is also known as the "shoelace formula".
     * @param xArray The array of x coordinates
     * @param yArray The array of y coordinates
     * @return The area of the object
     * @throws IllegalArgumentException if the array lengths are not equal
     */
    static double calculateSurveyorsArea(int[] xArray, int[] yArray) {
        if (xArray.length != yArray.length) {
            throw new IllegalArgumentException("Cannot calculate areas when arrays are uneven");
        }
        // Lines have no area
        if (xArray.length < 3) {
            return 0;
        }
        int area = 0;
        // Do the non-special stuff first (x0 * y1 - x1 * y0)
        for (int i = 0; i < xArray.length - 1; i++) {
            area += xArray[i] * yArray[i + 1] - xArray[i + 1] * yArray[i];
        }
        // Now calculate the edges (xn * y0 - x0 * yn)
        area += xArray[xArray.length - 1] * yArray[0] - xArray[0] * yArray[yArray.length - 1];
        return area / 2d;
    }

    /**
     * Get the shapes to draw this geometry with
     * @return A collection of shapes
     */
    public Collection<Shape> getShapes() {
        return Collections.unmodifiableCollection(this.shapes);
    }
}
