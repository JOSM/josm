// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Objects;

import org.openstreetmap.josm.gui.draw.SymbolShape;

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
     * The color to fill the interior of the shape.
     */
    public final Color fillColor;

    /**
     * Create a new symbol
     * @param symbol The symbol type
     * @param size The overall size of the symbol, both width and height are the same
     * @param stroke The stroke to use for the outline
     * @param strokeColor The color to draw the stroke with
     * @param fillColor The color to fill the interior of the shape.
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
        return symbolShape.shapeAround(x, y, size);
    }
}
