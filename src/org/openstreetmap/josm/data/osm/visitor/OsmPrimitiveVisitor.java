// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of the visitor pattern for the 3 {@link org.openstreetmap.josm.data.osm.OsmPrimitive}
 * types {@link Node}, {@link Way} and {@link Relation}.
 * @since 12810
 */
public interface OsmPrimitiveVisitor {
    /**
     * Visiting call for points.
     * @param n The node to inspect.
     */
    void visit(Node n);

    /**
     * Visiting call for lines.
     * @param w The way to inspect.
     * @since 64
     */
    void visit(Way w);

    /**
     * Visiting call for relations.
     * @param r The relation to inspect.
     * @since 343
     */
    void visit(Relation r);

}
