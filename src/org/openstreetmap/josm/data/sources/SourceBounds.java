// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.imagery.Shape;

/**
 *
 * Multi-polygon bounds for source backgrounds.
 * Used to display source coverage in preferences and to determine relevant source entries based on edit location.
 *
 * @author Frederik Ramm, extracted by Taylor Smock
 * @since 16545 (extracted from {@link org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds})
 */
public class SourceBounds extends Bounds {

    /**
     * Constructs a new {@code SourceBounds} from string.
     * @param asString The string containing the list of shapes defining this bounds
     * @param separator The shape separator in the given string, usually a comma
     */
    public SourceBounds(String asString, String separator) {
        super(asString, separator);
    }

    private List<Shape> shapes = new ArrayList<>();

    /**
     * Adds a new shape to this bounds.
     * @param shape The shape to add
     */
    public final void addShape(Shape shape) {
        this.shapes.add(shape);
    }

    /**
     * Sets the list of shapes defining this bounds.
     * @param shapes The list of shapes defining this bounds.
     */
    public final void setShapes(List<Shape> shapes) {
        this.shapes = shapes;
    }

    /**
     * Returns the list of shapes defining this bounds.
     * @return The list of shapes defining this bounds
     */
    public final List<Shape> getShapes() {
        return shapes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shapes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SourceBounds that = (SourceBounds) o;
        return Objects.equals(shapes, that.shapes);
    }
}
