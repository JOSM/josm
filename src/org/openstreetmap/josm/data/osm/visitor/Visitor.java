// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of the visitor scheme. Every OsmPrimitive can be visited by
 * several different visitors.
 *
 * @author imi
 */
public interface Visitor {
    void visit(Node n);
    void visit(Way w);
    void visit(Relation e);
    void visit(Changeset cs);
}
