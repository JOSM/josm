// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.draw;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.GuiSizesHelper;

/**
 * A list of possible symbol shapes.
 * @since 10875
 */
public enum SymbolShape {
    /**
     * A square
     */
    SQUARE("square", 4, Math.PI / 4),
    /**
     * A circle
     */
    CIRCLE("circle", 1, 0),
    /**
     * A triangle with sides of equal lengh
     */
    TRIANGLE("triangle", 3, Math.PI / 2),
    /**
     * A pentagon
     */
    PENTAGON("pentagon", 5, Math.PI / 2),
    /**
     * A hexagon
     */
    HEXAGON("hexagon", 6, 0),
    /**
     * A heptagon
     */
    HEPTAGON("heptagon", 7, Math.PI / 2),
    /**
     * An octagon
     */
    OCTAGON("octagon", 8, Math.PI / 8),
    /**
     * a nonagon
     */
    NONAGON("nonagon", 9, Math.PI / 2),
    /**
     * A decagon
     */
    DECAGON("decagon", 10, 0);

    private final String name;
    final int sides;

    final double rotation;

    SymbolShape(String name, int sides, double rotation) {
        this.name = name;
        this.sides = sides;
        this.rotation = rotation;
    }

    /**
     * Create the path for this shape around the given position
     * @param x The x position
     * @param y The y position
     * @param size The size (width for rect, diameter for rest)
     * @return The symbol.
     * @since 10875
     */
    public Shape shapeAround(double x, double y, double size) {
        size = GuiSizesHelper.getSizeDpiAdjusted(size);
        double radius = size / 2;
        Shape shape;
        switch (this) {
        case SQUARE:
            // optimize for performance reasons
            shape = new Rectangle2D.Double(x - radius, y - radius, size, size);
            break;
        case CIRCLE:
            shape = new Ellipse2D.Double(x - radius, y - radius, size, size);
            break;
        default:
            shape = buildPolygon(x, y, radius);
            break;
        }
        return shape;
    }

    private Shape buildPolygon(double cx, double cy, double radius) {
        GeneralPath polygon = new GeneralPath();
        for (int i = 0; i < sides; i++) {
            double angle = ((2 * Math.PI / sides) * i) - rotation;
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            if (i == 0) {
                polygon.moveTo(x, y);
            } else {
                polygon.lineTo(x, y);
            }
        }
        polygon.closePath();
        return polygon;
    }

    /**
     * Gets the number of normally straight sides this symbol has. Returns 1 for a circle.
     * @return The sides of the symbol
     */
    public int getSides() {
        return sides;
    }

    /**
     * Gets the rotation of the first point of this symbol.
     * @return The rotation
     */
    public double getRotation() {
        return rotation;
    }

    /**
     * Get the MapCSS name for this shape
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the shape with the given name
     * @param val The name to search
     * @return The shape as optional
     */
    public static Optional<SymbolShape> forName(String val) {
        return Stream.of(values()).filter(shape -> val.equals(shape.name)).findAny();
    }
}
