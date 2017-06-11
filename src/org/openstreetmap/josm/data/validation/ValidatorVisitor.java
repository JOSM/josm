// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.WaySegment;

/**
 * A visitor that is used during validation.
 * <p>
 * The most basic use is to visit all {@link TestError}s of the validator
 */
public interface ValidatorVisitor {
    /**
     * Visit a test error
     * @param error The test error to visit
     */
    void visit(TestError error);

    /**
     * Visit a OSM primitive, e.g. to highlight it
     * @param primitive The primitive
     */
    void visit(OsmPrimitive primitive);

    /**
     * Visit a way segment that was part of the error
     * @param waySegment The way segment
     */
    void visit(WaySegment waySegment);

    /**
     * Visit a list of nodes that are part of the error
     * @param nodes The nodes
     */
    void visit(List<Node> nodes);
}
