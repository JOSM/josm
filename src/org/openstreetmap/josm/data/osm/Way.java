// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.HashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.Pair;

/**
 * One full way, consisting of a list of way nodes.
 *
 * @author imi
 */
public final class Way extends OsmPrimitive {

    /**
     * All way nodes in this way
     */
    public final List<Node> nodes = new ArrayList<Node>();

    public void visitNodes(Visitor v) {
        for (Node n : this.nodes)
            v.visit(n);
    }

    public ArrayList<Pair<Node,Node>> getNodePairs(boolean sort) {
        ArrayList<Pair<Node,Node>> chunkSet = new ArrayList<Pair<Node,Node>>();
        Node lastN = null;
        for (Node n : this.nodes) {
            if (lastN == null) {
                lastN = n;
                continue;
            }
            Pair<Node,Node> np = new Pair<Node,Node>(lastN, n);
            if (sort) {
                Pair.sort(np);
            }
            chunkSet.add(np);
            lastN = n;
        }
        return chunkSet;
    }


    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Way(Way clone) {
            cloneFrom(clone);
    }

    /**
     * Create an empty way without id. Use this only if you set meaningful
     * values yourself.
     */
    public Way() {
    }

    /**
     * Create an incomplete Way.
     */
    public Way(long id) {
        this.id = id;
        incomplete = true;
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        nodes.clear();
        nodes.addAll(((Way)osm).nodes);
        checkDirectionTagged();
    }

    @Override public String toString() {
        return "{Way id="+id+" version="+version+" nodes="+Arrays.toString(nodes.toArray())+"}";
    }

    @Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
        return osm instanceof Way ? super.realEqual(osm, semanticOnly) && nodes.equals(((Way)osm).nodes) : false;
    }

    public int compareTo(OsmPrimitive o) {
        if(o instanceof Relation)
            return 1;
        return o instanceof Way ? Long.valueOf(id).compareTo(o.id) : -1;
    }

    public String getName() {
        String name;
        if (incomplete) {
            name = tr("incomplete");
        } else {
            name = get("name");
            if (name == null) name = get("ref");
            if (name == null) {
                name =
                    (get("highway") != null) ? tr("highway") :
                    (get("railway") != null) ? tr("railway") :
                    (get("waterway") != null) ? tr("waterway") :
                    (get("landuse") != null) ? tr("landuse") : "";
            }

            int nodesNo = new HashSet<Node>(nodes).size();
            String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
            name += (name.length() > 0) ? " ("+nodes+")" : nodes;
        }
        return name;
    }

    public Boolean isClosed() {
        int s = nodes.size();
        return s >= 3 && nodes.get(0) == nodes.get(s-1);
    }
}
