// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The definition of a symbol that should be rendered at the node position.
 * @since 10827 Extracted from {@link NodeElement}
 */
public class Symbol {
    private final SymbolShape symbolShape;
    /**
     * The width and height of this symbol
     */
    public final int size;
    /**
     * The stroke to use for the outline
     */
    public final Stroke stroke;
    /**
     * The color to draw the stroke with
     */
    public final Color strokeColor;
    /**
     * The color to fill the interiour of the shape.
     */
    public final Color fillColor;

    /**
     * Create a new symbol
     * @param symbol The symbol type
     * @param size The overall size of the symbol, both width and height are the same
     * @param stroke The stroke to use for the outline
     * @param strokeColor The color to draw the stroke with
     * @param fillColor The color to fill the interiour of the shape.
     */
    public Symbol(SymbolShape symbol, int size, Stroke stroke, Color strokeColor, Color fillColor) {
        if (stroke != null && strokeColor == null)
            throw new IllegalArgumentException("Stroke given without color");
        if (stroke == null && fillColor == null)
            throw new IllegalArgumentException("Either a stroke or a fill color must be given");
        this.symbolShape = symbol;
        this.size = size;
        this.stroke = stroke;
        this.strokeColor = strokeColor;
        this.fillColor = fillColor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final Symbol other = (Symbol) obj;
        return symbolShape == other.symbolShape &&
                size == other.size &&
                Objects.equals(stroke, other.stroke) &&
                Objects.equals(strokeColor, other.strokeColor) &&
                Objects.equals(fillColor, other.fillColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbolShape, size, stroke, strokeColor, fillColor);
    }

    @Override
    public String toString() {
        return "symbolShape=" + symbolShape + " size=" + size +
                (stroke != null ? (" stroke=" + stroke + " strokeColor=" + strokeColor) : "") +
                (fillColor != null ? (" fillColor=" + fillColor) : "");
    }

    /**
     * Builds the shape for this symbol
     * @param x The center x coordinate
     * @param y The center y coordinate
     * @return The symbol shape.
     */
    public Shape buildShapeAround(double x, double y) {
        int radius = size / 2;
        Shape shape;
        switch (symbolShape) {
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

    private Shape buildPolygon(double cx, double cy, int radius) {
        GeneralPath polygon = new GeneralPath();
        for (int i = 0; i < symbolShape.sides; i++) {
            double angle = ((2 * Math.PI / symbolShape.sides) * i) - symbolShape.rotation;
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
     * A list of possible symbol shapes.
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
         * Gets the number of normally straight sides this symbol has. Returns 1 for a circle.
         * @return The sides of the symbol
         */
        public int getSides() {
            return sides;
        }

        /**
         * Gets the rotateion of the first point of this symbol.
         * @return The roration
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
}
