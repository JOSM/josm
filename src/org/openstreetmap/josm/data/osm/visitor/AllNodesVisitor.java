// License: GPL. For details, see LICENSE file.
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
public class AllNodesVisitor implements OsmPrimitiveVisitor {

    /**
     * The resulting nodes collected so far.
     */
    public Collection<Node> nodes = new HashSet<>();

    /**
     * Nodes have only itself as nodes.
     */
    @Override
    public void visit(Node n) {
        nodes.add(n);
    }

    /**
     * Ways have their way nodes.
     */
    @Override
    public void visit(Way w) {
        if (w.isIncomplete()) return;
        for (Node n : w.getNodes()) {
            visit(n);
        }
    }

    /**
     * Relations may have any number of nodes.
     * FIXME: do we want to collect nodes from segs/ways that are relation members?
     * if so, use AutomatchVisitor!
     */
    @Override
    public void visit(Relation e) {
        for (RelationMember m : e.getMembers()) {
            if (m.isNode()) visit(m.getNode());
        }
    }

    /**
     * Replies all nodes contained by the given primitives
     * @param osms The OSM primitives to inspect
     * @return All nodes the given primitives have.
     */
    public static Collection<Node> getAllNodes(Collection<? extends OsmPrimitive> osms) {
        AllNodesVisitor v = new AllNodesVisitor();
        for (OsmPrimitive osm : osms) {
            osm.accept(v);
        }
        return v.nodes;
    }
}
