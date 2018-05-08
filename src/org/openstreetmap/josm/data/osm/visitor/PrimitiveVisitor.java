// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;

/**
 * OSM primitives interfaces visitor, following conventional <a href="https://en.wikipedia.org/wiki/Visitor_pattern">visitor design pattern</a>.
 * @since 4100
 */
public interface PrimitiveVisitor {

    /**
     * Visiting call for nodes.
     * @param n The node to inspect.
     */
    void visit(INode n);

    /**
     * Visiting call for ways.
     * @param w The way to inspect.
     */
    void visit(IWay<?> w);

    /**
     * Visiting call for relations.
     * @param r The relation to inspect.
     */
    void visit(IRelation r);
}
