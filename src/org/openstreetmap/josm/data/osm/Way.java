// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
     */
    private Node[] nodes = new Node[0];
    private BBox bbox;
    private boolean hasIncompleteNodes;

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
        for (Node node:this.nodes) {
            node.removeReferrer(this);
        }

        if (nodes == null) {
            this.nodes = new Node[0];
        } else {
            this.nodes = nodes.toArray(new Node[nodes.size()]);
        }
        for (Node node:this.nodes) {
            node.addReferrer(this);
        }

        clearCached();
        fireNodesChanged();
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
        if (isIncomplete()) return chunkSet;
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
     *
     * @param original
     * @param clearId
     */
    public Way(Way original, boolean clearId) {
        super(original.getUniqueId(), true);
        cloneFrom(original);
        if (clearId) {
            clearOsmId();
        }
    }

    /**
     * Create an identical clone of the argument (including the id).
     *
     * @param original  the original way. Must not be null.
     */
    public Way(Way original) {
        this(original, false);
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

    /**
     *
     * @param data
     */
    @Override
    public void load(PrimitiveData data) {
        super.load(data);

        WayData wayData = (WayData) data;

        List<Node> newNodes = new ArrayList<Node>(wayData.getNodes().size());
        for (Long nodeId : wayData.getNodes()) {
            Node node = (Node)getDataSet().getPrimitiveById(nodeId, OsmPrimitiveType.NODE);
            if (node != null) {
                newNodes.add(node);
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
        setNodes(otherWay.getNodes());
    }

    @Override public String toString() {
        String nodesDesc = isIncomplete()?"(incomplete)":"nodes=" + Arrays.toString(nodes);
        return "{Way id=" + getUniqueId() + " version=" + getVersion()+ " " + getFlagsAsString()  + " " + nodesDesc + "}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Way) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Way w = (Way)other;
        if (getNodesCount() != w.getNodesCount()) return false;
        for (int i=0;i<getNodesCount();i++) {
            if (! getNode(i).hasEqualSemanticAttributes(w.getNode(i)))
                return false;
        }
        return true;
    }

    public int compareTo(OsmPrimitive o) {
        if (o instanceof Relation)
            return 1;
        return o instanceof Way ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : -1;
    }

    public void removeNode(Node n) {
        if (isIncomplete()) return;
        boolean closed = (lastNode() == n && firstNode() == n);
        int i;
        List<Node> copy = getNodes();
        while ((i = copy.indexOf(n)) >= 0) {
            copy.remove(i);
        }
        i = copy.size();
        if (closed && i > 2) {
            copy.add(copy.get(0));
        } else if (i >= 2 && i <= 3 && copy.get(0) == copy.get(i-1)) {
            copy.remove(i-1);
        }
        setNodes(copy);
    }

    public void removeNodes(Collection<? extends OsmPrimitive> selection) {
        if (isIncomplete()) return;
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
        if (isIncomplete())
            throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));
        clearCached();
        n.addReferrer(this);
        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
        newNodes[nodes.length] = n;
        nodes = newNodes;
        fireNodesChanged();
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
        if (isIncomplete())
            throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));
        clearCached();
        n.addReferrer(this);
        Node[] newNodes = new Node[nodes.length + 1];
        System.arraycopy(nodes, 0, newNodes, 0, offs);
        System.arraycopy(nodes, offs, newNodes, offs + 1, nodes.length - offs);
        newNodes[offs] = n;
        nodes = newNodes;
        fireNodesChanged();
    }

    @Override
    public void setDeleted(boolean deleted) {
        for (Node n:nodes) {
            if (deleted) {
                n.removeReferrer(this);
            } else {
                n.addReferrer(this);
            }
        }
        fireNodesChanged();
        super.setDeleted(deleted);
    }

    public boolean isClosed() {
        if (isIncomplete()) return false;
        return nodes.length >= 3 && lastNode() == firstNode();
    }

    public Node lastNode() {
        if (isIncomplete() || nodes.length == 0) return null;
        return nodes[nodes.length-1];
    }

    public Node firstNode() {
        if (isIncomplete() || nodes.length == 0) return null;
        return nodes[0];
    }

    public boolean isFirstLastNode(Node n) {
        if (isIncomplete() || nodes.length == 0) return false;
        return n == firstNode() || n == lastNode();
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }

    private void fireNodesChanged() {
        if (getDataSet() != null) {
            getDataSet().fireWayNodesChanged(this);
        }
    }

    @Override
    public BBox getBBox() {
        if (getDataSet() == null)
            return new BBox(this);
        if (bbox == null) {
            bbox = new BBox(this);
        }
        return bbox;
    }

    @Override
    public void updatePosition() {
        bbox = new BBox(this);
    }

    //TODO This method should not be necessary. hasIncomplete state should be updated automatically when incomplete state of nodes change
    public void setHasIncompleteNodes() {
        hasIncompleteNodes = false;
        for (Node node:getNodes()) {
            if (node.isIncomplete()) {
                hasIncompleteNodes = true;
                break;
            }
        }
    }

    public boolean hasIncompleteNodes() {
        return hasIncompleteNodes;
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && !hasIncompleteNodes();
    }
}
