// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.HashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    /* mappaint data */
    public boolean isMappaintArea = false;
    public Integer mappaintDrawnAreaCode = 0;
    /* end of mappaint data */
    @Override protected void clearCached() {
        super.clearCached();
        isMappaintArea = false;
        mappaintDrawnAreaCode = 0;
    }

    public void visitNodes(Visitor v) {
        if (incomplete) return;
        for (Node n : this.nodes)
            v.visit(n);
    }

    public ArrayList<Pair<Node,Node>> getNodePairs(boolean sort) {
        ArrayList<Pair<Node,Node>> chunkSet = new ArrayList<Pair<Node,Node>>();
        if (incomplete) return chunkSet;
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
    }

    @Override public String toString() {
        if (incomplete) return "{Way id="+id+" version="+version+" (incomplete)}";
        return "{Way id="+id+" version="+version+" nodes="+Arrays.toString(nodes.toArray())+"}";
    }

    @Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
        return osm instanceof Way ? super.realEqual(osm, semanticOnly) && nodes.equals(((Way)osm).nodes) : false;
    }

    public int compareTo(OsmPrimitive o) {
        if (o instanceof Relation)
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
            if(errors != null)
                name = "*"+name;
        }
        return name;
    }

    public void removeNode(Node n) {
        if (incomplete) return;
        boolean closed = (lastNode() == n && firstNode() == n);
        int i;
        while ((i = nodes.indexOf(n)) >= 0)
            nodes.remove(i);
        i = nodes.size();
        if (closed && i > 2) // close again
            addNode(firstNode());
        // prevent closed ways with less than 3 different nodes
        else if (i >= 2 && i <= 3 && nodes.get(0) == nodes.get(i-1))
            nodes.remove(i-1);
    }

    public void removeNodes(Collection<? extends OsmPrimitive> selection) {
        if (incomplete) return;
        for(OsmPrimitive p : selection) {
           if (p instanceof Node) {
               removeNode((Node)p);
           }
       }
    }

    public void addNode(Node n) {
        if (incomplete) return;
        clearCached();
        nodes.add(n);
    }

    public void addNode(int offs, Node n) {
        if (incomplete) return;
        clearCached();
        nodes.add(offs, n);
    }

    public boolean isClosed() {
        if (incomplete) return false;
        return nodes.size() >= 3 && lastNode() == firstNode();
    }

    public Node lastNode() {
        if (incomplete || nodes.size() == 0) return null;
        return nodes.get(nodes.size()-1);
    }

    public Node firstNode() {
        if (incomplete || nodes.size() == 0) return null;
        return nodes.get(0);
    }

    public boolean isFirstLastNode(Node n) {
        if (incomplete || nodes.size() == 0) return false;
        return n == firstNode() || n == lastNode();
    }
}
