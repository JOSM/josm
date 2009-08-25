// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.CopyList;
import org.openstreetmap.josm.tools.Pair;

/**
 * One full way, consisting of a list of way nodes.
 *
 * @author imi
 */
public final class Way extends OsmPrimitive {

    /**
     * All way nodes in this way
     *
     * @deprecated This public field will become private or protected in the future.
     *  Use the new public API instead.
     */
    @Deprecated
    public final List<Node> nodes = new ArrayList<Node>();

    /**
     *
     * You can modify returned list but changes will not be propagated back
     * to the Way. Use {@link #setNodes(List)} to update this way
     * @return Nodes composing the way
     * @since 1862
     */
    public List<Node> getNodes() {
        return new CopyList<Node>(nodes.toArray(new Node[nodes.size()]));
    }

    /**
     * @param nodes New way nodes. Can be null, in that case all way nodes are removed
     * @since 1862
     */
    public void setNodes(List<Node> nodes) {
        this.nodes.clear();
        if (nodes != null) {
            this.nodes.addAll(nodes);
        }
        clearCached();
    }

    /**
     * Replies the number of nodes in this ways.
     * 
     * @return the number of nodes in this ways.
     * @since 1862
     */
    public int getNodesCount() {
        return nodes.size();
    }

    /**
     * Replies the node at position <code>index</code>.
     * 
     * @param index the position
     * @return  the node at position <code>index</code>
     * @exception IndexOutOfBoundsException thrown if <code>index</code> < 0
     * or <code>index</code> >= {@see #getNodesCount()}
     * @since 1862
     */
    public Node getNode(int index) {
        return nodes.get(index);
    }

    /**
     * Replies true if this way contains the node <code>node</code>, false
     * otherwise. Replies false if  <code>node</code> is null.
     * 
     * @param node the node. May be null.
     * @return true if this way contains the node <code>node</code>, false
     * otherwise
     * @since 1909
     */
    public boolean containsNode(Node node) {
        if (node == null) return false;
        return nodes.contains(node);
    }

    /* mappaint data */
    public boolean isMappaintArea = false;
    public Integer mappaintDrawnAreaCode = 0;
    /* end of mappaint data */
    @Override protected void clearCached() {
        super.clearCached();
        isMappaintArea = false;
        mappaintDrawnAreaCode = 0;
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
     * Create an identical clone of the argument (including the id).
     * 
     * @param original  the original way. Must not be null.
     */
    public Way(Way original) {
        cloneFrom(original);
    }

    /**
     * Create an empty way without id. Use this only if you set meaningful
     * values yourself.
     */
    public Way() {
    }

    /**
     * Create an incomplete Way with a given id.
     * 
     * @param id  the id. id > 0 required.
     */
    public Way(long id) {
        // FIXME: shouldn't we check for id > 0?
        //
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

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Way) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Way w = (Way)other;
        return nodes.equals(w.nodes);
    }

    public int compareTo(OsmPrimitive o) {
        if (o instanceof Relation)
            return 1;
        return o instanceof Way ? Long.valueOf(id).compareTo(o.id) : -1;
    }

    public void removeNode(Node n) {
        if (incomplete) return;
        boolean closed = (lastNode() == n && firstNode() == n);
        int i;
        while ((i = nodes.indexOf(n)) >= 0) {
            nodes.remove(i);
        }
        i = nodes.size();
        if (closed && i > 2) {
            addNode(firstNode());
        } else if (i >= 2 && i <= 3 && nodes.get(0) == nodes.get(i-1)) {
            nodes.remove(i-1);
        }
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

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }
}
