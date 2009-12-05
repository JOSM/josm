// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Collect all nodes a specific osm primitive has.
 *
 * @author imi
 */
public class AllNodesVisitor extends AbstractVisitor {

    /**
     * The resulting nodes collected so far.
     */
    public Collection<Node> nodes = new HashSet<Node>();

    /**
     * Nodes have only itself as nodes.
     */
    public void visit(Node n) {
        nodes.add(n);
    }

    /**
     * Ways have their way nodes.
     */
    public void visit(Way w) {
        if (w.isIncomplete()) return;
        for (Node n : w.getNodes())
            visit(n);
    }

    /**
     * Relations may have any number of nodes.
     * FIXME: do we want to collect nodes from segs/ways that are relation members?
     * if so, use AutomatchVisitor!
     */
    public void visit(Relation e) {
        for (RelationMember m : e.getMembers())
            if (m.isNode()) visit(m.getNode());
    }
    /**
     * @return All nodes the given primitive has.
     */
    public static Collection<Node> getAllNodes(Collection<? extends OsmPrimitive> osms) {
        AllNodesVisitor v = new AllNodesVisitor();
        for (OsmPrimitive osm : osms)
            osm.visit(v);
        return v.nodes;
    }
}
