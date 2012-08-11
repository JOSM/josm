// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of the visitor scheme. Every @{link org.openstreetmap.josm.data.OsmPrimitive}
 * can be visited by several different visitors.
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
     */
    void visit(Way w);
    /**
     * Visiting call for relations.
     * @param e The relation to inspect.
     */
    void visit(Relation e);
    /**
     * Visiting call for changesets.
     * @param cs The changeset to inspect.
     */
    void visit(Changeset cs);
}
