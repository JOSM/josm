// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of the visitor scheme. Every @{link org.openstreetmap.josm.data.OsmPrimitive}
 * can be visited by several different visitors.
 * @since 8
 */
public interface Visitor {
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
    /**
     * Visiting call for changesets.
     * @param cs The changeset to inspect.
     * @since 1523
     */
    void visit(Changeset cs);
}
