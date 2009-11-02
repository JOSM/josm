// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     */
    private Node[] nodes = new Node[0];

    /**
     *
     * You can modify returned list but changes will not be propagated back
     * to the Way. Use {@link #setNodes(List)} to update this way
     * @return Nodes composing the way
     * @since 1862
     */
    public List<Node> getNodes() {
        return new CopyList<Node>(nodes);
    }

    /**
     * Set new list of nodes to way. This method is preferred to multiple calls to addNode/removeNode
     * and similar methods because nodes are internally saved as array which means lower memory overhead
     * but also slower modifying operations.
     * @param nodes New way nodes. Can be null, in that case all way nodes are removed
     * @since 1862
     */
    public void setNodes(List<Node> nodes) {
        if (nodes == null) {
            this.nodes = new Node[0];
        } else {
            this.nodes = nodes.toArray(new Node[nodes.size()]);
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
        return nodes.length;
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
        return nodes[index];
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
        for (int i=0; i<nodes.length; i++) {
            if (nodes[i].equals(node))
                return true;
        }
        return false;
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

    protected Way(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Creates a new way with id 0.
     *
     */
    public Way(){
        super(0, false);
    }

    /**
     * Create an identical clone of the argument (including the id).
     *
     * @param original  the original way. Must not be null.
     */
    public Way(Way original) {
        super(original.getUniqueId(), true);
        cloneFrom(original);
    }

    /**
     * Creates a new way for the given id. If the id > 0, the way is marked
     * as incomplete.
     *
     * @param id the id. > 0 required
     * @throws IllegalArgumentException thrown if id < 0
     */
    public Way(long id) throws IllegalArgumentException {
        super(id, false);
    }

    public Way(WayData data, DataSet dataSet) {
        super(data);
        load(data, dataSet);
    }

    /**
     *
     * @param data
     * @param dataSet Dataset this way is part of. This parameter will be removed in future
     */
    @Override
    public void load(PrimitiveData data, DataSet dataSet) {
        super.load(data, dataSet);

        WayData wayData = (WayData) data;

        // TODO We should have some lookup by id mechanism in future to speed this up
        Node marker = new Node(0);
        Map<Long, Node> foundNodes = new HashMap<Long, Node>();
        for (Long nodeId : wayData.getNodes()) {
            foundNodes.put(nodeId, marker);
        }
        for (Node node : dataSet.getNodes()) {
            if (foundNodes.get(node.getUniqueId()) == marker) {
                foundNodes.put(node.getUniqueId(), node);
            }
        }

        List<Node> newNodes = new ArrayList<Node>(wayData.getNodes().size());
        for (Long nodeId : wayData.getNodes()) {
            Node node = foundNodes.get(nodeId);
            if (node != marker) {
                newNodes.add(foundNodes.get(nodeId));
            } else
                throw new AssertionError("Data consistency problem - way with missing node detected");
        }
        setNodes(newNodes);
    }

    @Override public WayData save() {
        WayData data = new WayData();
        saveCommonAttributes(data);
        for (Node node:getNodes()) {
            data.getNodes().add(node.getUniqueId());
        }
        return data;
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        Way otherWay = (Way)osm;
        nodes = new Node[otherWay.nodes.length];
        System.arraycopy(otherWay.nodes, 0, nodes, 0, otherWay.nodes.length);
    }

    @Override public String toString() {
        String nodesDesc = incomplete?"(incomplete)":"nodes=" + Arrays.toString(nodes);
        return "{Way id=" + getUniqueId() + " version=" + getVersion()+ " " + getFlagsAsString()  + " " + nodesDesc + "}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Way) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Way w = (Way)other;
        return Arrays.equals(nodes, w.nodes);
    }

    public int compareTo(OsmPrimitive o) {
        if (o instanceof Relation)
            return 1;
        return o instanceof Way ? Long.valueOf(getId()).compareTo(o.getId()) : -1;
    }

    public void removeNode(Node n) {
        if (incomplete) return;
        boolean closed = (lastNode() == n && firstNode() == n);
        int i;
        List<Node> copy = getNodes();
        while ((i = copy.indexOf(n)) >= 0) {
            copy.remove(i);
        }
        i = copy.size();
        if (closed && i > 2) {
            addNode(firstNode());
        } else if (i >= 2 && i <= 3 && copy.get(0) == copy.get(i-1)) {
            copy.remove(i-1);
        }
        setNodes(copy);
    }

    public void removeNodes(Collection<? extends OsmPrimitive> selection) {
        if (incomplete) return;
        for(OsmPrimitive p : selection) {
            if (p instanceof Node) {
                removeNode((Node)p);
            }
        }
    }

    /**
     * Adds a node to the end of the list of nodes. Ignored, if n is null.
     *
     * @param n the node. Ignored, if null.
     * @throws IllegalStateException thrown, if this way is marked as incomplete. We can't add a node
     * to an incomplete way
     */
    public void addNode(Node n) throws IllegalStateException {
        if (n==null) return;
        if (incomplete)
            throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));
        clearCached();
        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
        newNodes[nodes.length] = n;
        nodes = newNodes;
    }

    /**
     * Adds a node at position offs.
     *
     * @param int offs the offset
     * @param n the node. Ignored, if null.
     * @throws IllegalStateException thrown, if this way is marked as incomplete. We can't add a node
     * to an incomplete way
     * @throws IndexOutOfBoundsException thrown if offs is out of bounds
     */
    public void addNode(int offs, Node n) throws IllegalStateException, IndexOutOfBoundsException {
        if (n==null) return;
        if (incomplete)
            throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));
        clearCached();
        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, offs);
        System.arraycopy(nodes, offs, newNodes, offs + 1, nodes.length - offs);
        newNodes[offs] = n;
        nodes = newNodes;
    }

    public boolean isClosed() {
        if (incomplete) return false;
        return nodes.length >= 3 && lastNode() == firstNode();
    }

    public Node lastNode() {
        if (incomplete || nodes.length == 0) return null;
        return nodes[nodes.length-1];
    }

    public Node firstNode() {
        if (incomplete || nodes.length == 0) return null;
        return nodes[0];
    }

    public boolean isFirstLastNode(Node n) {
        if (incomplete || nodes.length == 0) return false;
        return n == firstNode() || n == lastNode();
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }
}
