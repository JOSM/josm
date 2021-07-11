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
    final Collection<Shape> shapes = new ArrayList<>();

    /**
     * Create a {@link Geometry} for a {@link Feature}
     * @param geometryType The type of geometry
     * @param commands The commands used to create the geometry
     * @throws IllegalArgumentException if arguments are not understood or if the shoelace formula returns 0 for a polygon ring.
     */
    public Geometry(GeometryTypes geometryType, List<CommandInteger> commands) {
        if (geometryType == GeometryTypes.POINT) {
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
        } else if (geometryType == GeometryTypes.LINESTRING || geometryType == GeometryTypes.POLYGON) {
            Path2D.Float line = null;
            Area area = null;
            // MVT uses delta encoding. Each feature starts at (0, 0).
            double x = 0;
            double y = 0;
            // Area is used to determine the inner/outer of a polygon
            double areaAreaSq = 0;
            for (CommandInteger command : commands) {
                final short[] operations = command.getOperations();
                // Technically, there is no reason why there can be multiple MoveTo operations in one command, but that is undefined behavior
                if (command.getType() == Command.MoveTo && operations.length == 2) {
                    areaAreaSq = 0;
                    x += operations[0];
                    y += operations[1];
                    line = new Path2D.Float();
                    line.moveTo(x, y);
                    shapes.add(line);
                } else if (command.getType() == Command.LineTo && operations.length % 2 == 0 && line != null) {
                    for (int i = 0; i < operations.length / 2; i++) {
                        final double lx = x;
                        final double ly = y;
                        x += operations[2 * i];
                        y += operations[2 * i + 1];
                        areaAreaSq += lx * y - x * ly;
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

                    Area nArea = new Area(line);
                    // SonarLint thinks that this is never > 0. It can be.
                    if (areaAreaSq > 0) {
                        area.add(nArea);
                    } else if (areaAreaSq < 0) {
                        area.exclusiveOr(nArea);
                    } else {
                        throw new IllegalArgumentException(tr("{0} cannot have zero area", geometryType));
                    }
                } else {
                    throw new IllegalArgumentException(tr("{0} with {1} arguments is not understood", geometryType, operations.length));
                }
            }
        }
    }

    /**
     * Get the shapes to draw this geometry with
     * @return A collection of shapes
     */
    public Collection<Shape> getShapes() {
        return Collections.unmodifiableCollection(this.shapes);
    }
}
