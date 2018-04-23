// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CopyList;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * One full way, consisting of a list of way {@link Node nodes}.
 *
 * @author imi
 * @since 64
 */
public final class Way extends OsmPrimitive implements IWay {

    /**
     * All way nodes in this way
     *
     */
    private Node[] nodes = new Node[0];
    private BBox bbox;

    /**
     *
     * You can modify returned list but changes will not be propagated back
     * to the Way. Use {@link #setNodes(List)} to update this way
     * @return Nodes composing the way
     * @since 1862
     */
    public List<Node> getNodes() {
        return new CopyList<>(nodes);
    }

    /**
     * Set new list of nodes to way. This method is preferred to multiple calls to addNode/removeNode
     * and similar methods because nodes are internally saved as array which means lower memory overhead
     * but also slower modifying operations.
     * @param nodes New way nodes. Can be null, in that case all way nodes are removed
     * @since 1862
     */
    public void setNodes(List<Node> nodes) {
        checkDatasetNotReadOnly();
        boolean locked = writeLock();
        try {
            for (Node node:this.nodes) {
                node.removeReferrer(this);
                node.clearCachedStyle();
            }

            if (nodes == null) {
                this.nodes = new Node[0];
            } else {
                this.nodes = nodes.toArray(new Node[0]);
            }
            for (Node node: this.nodes) {
                node.addReferrer(this);
                node.clearCachedStyle();
            }

            clearCachedStyle();
            fireNodesChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Prevent directly following identical nodes in ways.
     * @param nodes list of nodes
     * @return {@code nodes} with consecutive identical nodes removed
     */
    private static List<Node> removeDouble(List<Node> nodes) {
        Node last = null;
        int count = nodes.size();
        for (int i = 0; i < count && count > 2;) {
            Node n = nodes.get(i);
            if (last == n) {
                nodes.remove(i);
                --count;
            } else {
                last = n;
                ++i;
            }
        }
        return nodes;
    }

    @Override
    public int getNodesCount() {
        return nodes.length;
    }

    /**
     * Replies the node at position <code>index</code>.
     *
     * @param index the position
     * @return  the node at position <code>index</code>
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> &lt; 0
     * or <code>index</code> &gt;= {@link #getNodesCount()}
     * @since 1862
     */
    public Node getNode(int index) {
        return nodes[index];
    }

    @Override
    public long getNodeId(int idx) {
        return nodes[idx].getUniqueId();
    }

    /**
     * Replies true if this way contains the node <code>node</code>, false
     * otherwise. Replies false if  <code>node</code> is null.
     *
     * @param node the node. May be null.
     * @return true if this way contains the node <code>node</code>, false
     * otherwise
     * @since 1911
     */
    public boolean containsNode(Node node) {
        if (node == null) return false;

        Node[] nodes = this.nodes;
        for (Node n : nodes) {
            if (n.equals(node))
                return true;
        }
        return false;
    }

    /**
     * Return nodes adjacent to <code>node</code>
     *
     * @param node the node. May be null.
     * @return Set of nodes adjacent to <code>node</code>
     * @since 4671
     */
    public Set<Node> getNeighbours(Node node) {
        Set<Node> neigh = new HashSet<>();

        if (node == null) return neigh;

        Node[] nodes = this.nodes;
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].equals(node)) {
                if (i > 0)
                    neigh.add(nodes[i-1]);
                if (i < nodes.length-1)
                    neigh.add(nodes[i+1]);
            }
        }
        return neigh;
    }

    /**
     * Replies the ordered {@link List} of chunks of this way. Each chunk is replied as a {@link Pair} of {@link Node nodes}.
     * @param sort If true, the nodes of each pair are sorted as defined by {@link Pair#sort}.
     *             If false, Pair.a and Pair.b are in the way order
     *             (i.e for a given Pair(n), Pair(n-1).b == Pair(n).a, Pair(n).b == Pair(n+1).a, etc.)
     * @return The ordered list of chunks of this way.
     * @since 3348
     */
    public List<Pair<Node, Node>> getNodePairs(boolean sort) {
        List<Pair<Node, Node>> chunkSet = new ArrayList<>();
        if (isIncomplete()) return chunkSet;
        Node lastN = null;
        Node[] nodes = this.nodes;
        for (Node n : nodes) {
            if (lastN == null) {
                lastN = n;
                continue;
            }
            Pair<Node, Node> np = new Pair<>(lastN, n);
            if (sort) {
                Pair.sort(np);
            }
            chunkSet.add(np);
            lastN = n;
        }
        return chunkSet;
    }

    @Override public void accept(OsmPrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    protected Way(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Contructs a new {@code Way} with id 0.
     * @since 86
     */
    public Way() {
        super(0, false);
    }

    /**
     * Contructs a new {@code Way} from an existing {@code Way}.
     * @param original The original {@code Way} to be identically cloned. Must not be null
     * @param clearMetadata If {@code true}, clears the OSM id and other metadata as defined by {@link #clearOsmMetadata}.
     * If {@code false}, does nothing
     * @since 2410
     */
    public Way(Way original, boolean clearMetadata) {
        super(original.getUniqueId(), true);
        cloneFrom(original);
        if (clearMetadata) {
            clearOsmMetadata();
        }
    }

    /**
     * Contructs a new {@code Way} from an existing {@code Way} (including its id).
     * @param original The original {@code Way} to be identically cloned. Must not be null
     * @since 86
     */
    public Way(Way original) {
        this(original, false);
    }

    /**
     * Contructs a new {@code Way} for the given id. If the id &gt; 0, the way is marked
     * as incomplete. If id == 0 then way is marked as new
     *
     * @param id the id. &gt;= 0 required
     * @throws IllegalArgumentException if id &lt; 0
     * @since 343
     */
    public Way(long id) {
        super(id, false);
    }

    /**
     * Contructs a new {@code Way} with given id and version.
     * @param id the id. &gt;= 0 required
     * @param version the version
     * @throws IllegalArgumentException if id &lt; 0
     * @since 2620
     */
    public Way(long id, int version) {
        super(id, version, false);
    }

    @Override
    public void load(PrimitiveData data) {
        if (!(data instanceof WayData))
            throw new IllegalArgumentException("Not a way data: " + data);
        boolean locked = writeLock();
        try {
            super.load(data);

            WayData wayData = (WayData) data;

            if (!wayData.getNodes().isEmpty() && getDataSet() == null) {
                throw new AssertionError("Data consistency problem - way without dataset detected");
            }

            List<Node> newNodes = new ArrayList<>(wayData.getNodes().size());
            for (Long nodeId : wayData.getNodes()) {
                Node node = (Node) getDataSet().getPrimitiveById(nodeId, OsmPrimitiveType.NODE);
                if (node != null) {
                    newNodes.add(node);
                } else {
                    throw new AssertionError("Data consistency problem - way with missing node detected");
                }
            }
            setNodes(newNodes);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public WayData save() {
        WayData data = new WayData();
        saveCommonAttributes(data);
        for (Node node:nodes) {
            data.getNodes().add(node.getUniqueId());
        }
        return data;
    }

    @Override
    public void cloneFrom(OsmPrimitive osm) {
        if (!(osm instanceof Way))
            throw new IllegalArgumentException("Not a way: " + osm);
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            Way otherWay = (Way) osm;
            setNodes(otherWay.getNodes());
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public String toString() {
        String nodesDesc = isIncomplete() ? "(incomplete)" : ("nodes=" + Arrays.toString(nodes));
        return "{Way id=" + getUniqueId() + " version=" + getVersion()+ ' ' + getFlagsAsString() + ' ' + nodesDesc + '}';
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other, boolean testInterestingTagsOnly) {
        if (!(other instanceof Way))
            return false;
        Way w = (Way) other;
        if (getNodesCount() != w.getNodesCount()) return false;
        if (!super.hasEqualSemanticAttributes(other, testInterestingTagsOnly))
            return false;
        for (int i = 0; i < getNodesCount(); i++) {
            if (!getNode(i).hasEqualSemanticAttributes(w.getNode(i)))
                return false;
        }
        return true;
    }

    /**
     * Removes the given {@link Node} from this way. Ignored, if n is null.
     * @param n The node to remove. Ignored, if null
     * @since 1463
     */
    public void removeNode(Node n) {
        checkDatasetNotReadOnly();
        if (n == null || isIncomplete()) return;
        boolean locked = writeLock();
        try {
            boolean closed = lastNode() == n && firstNode() == n;
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
            setNodes(removeDouble(copy));
            n.clearCachedStyle();
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Removes the given set of {@link Node nodes} from this way. Ignored, if selection is null.
     * @param selection The selection of nodes to remove. Ignored, if null
     * @since 5408
     */
    public void removeNodes(Set<? extends Node> selection) {
        checkDatasetNotReadOnly();
        if (selection == null || isIncomplete()) return;
        boolean locked = writeLock();
        try {
            boolean closed = isClosed() && selection.contains(lastNode());
            List<Node> copy = new ArrayList<>();

            for (Node n: nodes) {
                if (!selection.contains(n)) {
                    copy.add(n);
                }
            }

            int i = copy.size();
            if (closed && i > 2) {
                copy.add(copy.get(0));
            } else if (i >= 2 && i <= 3 && copy.get(0) == copy.get(i-1)) {
                copy.remove(i-1);
            }
            setNodes(removeDouble(copy));
            for (Node n : selection) {
                n.clearCachedStyle();
            }
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Adds a node to the end of the list of nodes. Ignored, if n is null.
     *
     * @param n the node. Ignored, if null
     * @throws IllegalStateException if this way is marked as incomplete. We can't add a node
     * to an incomplete way
     * @since 1313
     */
    public void addNode(Node n) {
        checkDatasetNotReadOnly();
        if (n == null) return;

        boolean locked = writeLock();
        try {
            if (isIncomplete())
                throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));
            clearCachedStyle();
            n.addReferrer(this);
            nodes = Utils.addInArrayCopy(nodes, n);
            n.clearCachedStyle();
            fireNodesChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Adds a node at position offs.
     *
     * @param offs the offset
     * @param n the node. Ignored, if null.
     * @throws IllegalStateException if this way is marked as incomplete. We can't add a node
     * to an incomplete way
     * @throws IndexOutOfBoundsException if offs is out of bounds
     * @since 1313
     */
    public void addNode(int offs, Node n) {
        checkDatasetNotReadOnly();
        if (n == null) return;

        boolean locked = writeLock();
        try {
            if (isIncomplete())
                throw new IllegalStateException(tr("Cannot add node {0} to incomplete way {1}.", n.getId(), getId()));

            clearCachedStyle();
            n.addReferrer(this);
            Node[] newNodes = new Node[nodes.length + 1];
            System.arraycopy(nodes, 0, newNodes, 0, offs);
            System.arraycopy(nodes, offs, newNodes, offs + 1, nodes.length - offs);
            newNodes[offs] = n;
            nodes = newNodes;
            n.clearCachedStyle();
            fireNodesChanged();
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void setDeleted(boolean deleted) {
        boolean locked = writeLock();
        try {
            for (Node n:nodes) {
                if (deleted) {
                    n.removeReferrer(this);
                } else {
                    n.addReferrer(this);
                }
                n.clearCachedStyle();
            }
            fireNodesChanged();
            super.setDeleted(deleted);
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public boolean isClosed() {
        if (isIncomplete()) return false;

        Node[] nodes = this.nodes;
        return nodes.length >= 3 && nodes[nodes.length-1] == nodes[0];
    }

    /**
     * Determines if this way denotes an area (closed way with at least three distinct nodes).
     * @return {@code true} if this way is closed and contains at least three distinct nodes
     * @see #isClosed
     * @since 5490
     */
    public boolean isArea() {
        if (this.nodes.length >= 4 && isClosed()) {
            Node distinctNode = null;
            for (int i = 1; i < nodes.length-1; i++) {
                if (distinctNode == null && nodes[i] != nodes[0]) {
                    distinctNode = nodes[i];
                } else if (distinctNode != null && nodes[i] != nodes[0] && nodes[i] != distinctNode) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the last node of this way.
     * The result equals <code>{@link #getNode getNode}({@link #getNodesCount getNodesCount} - 1)</code>.
     * @return the last node of this way
     * @since 1400
     */
    public Node lastNode() {
        Node[] nodes = this.nodes;
        if (isIncomplete() || nodes.length == 0) return null;
        return nodes[nodes.length-1];
    }

    /**
     * Returns the first node of this way.
     * The result equals {@link #getNode getNode}{@code (0)}.
     * @return the first node of this way
     * @since 1400
     */
    public Node firstNode() {
        Node[] nodes = this.nodes;
        if (isIncomplete() || nodes.length == 0) return null;
        return nodes[0];
    }

    /**
     * Replies true if the given node is the first or the last one of this way, false otherwise.
     * @param n The node to test
     * @return true if the {@code n} is the first or the last node, false otherwise.
     * @since 1400
     */
    public boolean isFirstLastNode(Node n) {
        Node[] nodes = this.nodes;
        if (isIncomplete() || nodes.length == 0) return false;
        return n == nodes[0] || n == nodes[nodes.length -1];
    }

    /**
     * Replies true if the given node is an inner node of this way, false otherwise.
     * @param n The node to test
     * @return true if the {@code n} is an inner node, false otherwise.
     * @since 3515
     */
    public boolean isInnerNode(Node n) {
        Node[] nodes = this.nodes;
        if (isIncomplete() || nodes.length <= 2) return false;
        /* circular ways have only inner nodes, so return true for them! */
        if (n == nodes[0] && n == nodes[nodes.length-1]) return true;
        for (int i = 1; i < nodes.length - 1; ++i) {
            if (nodes[i] == n) return true;
        }
        return false;
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }

    @Override
    public OsmPrimitiveType getDisplayType() {
        return isClosed() ? OsmPrimitiveType.CLOSEDWAY : OsmPrimitiveType.WAY;
    }

    private void checkNodes() {
        DataSet dataSet = getDataSet();
        if (dataSet != null) {
            Node[] nodes = this.nodes;
            for (Node n: nodes) {
                if (n.getDataSet() != dataSet)
                    throw new DataIntegrityProblemException("Nodes in way must be in the same dataset",
                            tr("Nodes in way must be in the same dataset"));
                if (n.isDeleted())
                    throw new DataIntegrityProblemException("Deleted node referenced: " + toString(),
                            "<html>" + tr("Deleted node referenced by {0}",
                                    DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(this)) + "</html>");
            }
            if (Config.getPref().getBoolean("debug.checkNullCoor", true)) {
                for (Node n: nodes) {
                    if (n.isVisible() && !n.isIncomplete() && !n.isLatLonKnown())
                        throw new DataIntegrityProblemException("Complete visible node with null coordinates: " + toString(),
                                "<html>" + tr("Complete node {0} with null coordinates in way {1}",
                                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(n),
                                DefaultNameFormatter.getInstance().formatAsHtmlUnorderedList(this)) + "</html>");
                }
            }
        }
    }

    private void fireNodesChanged() {
        checkNodes();
        if (getDataSet() != null) {
            getDataSet().fireWayNodesChanged(this);
        }
    }

    @Override
    void setDataset(DataSet dataSet) {
        super.setDataset(dataSet);
        checkNodes();
    }

    @Override
    public BBox getBBox() {
        if (getDataSet() == null)
            return new BBox(this);
        if (bbox == null) {
            bbox = new BBox(this);
        }
        return new BBox(bbox);
    }

    @Override
    protected void addToBBox(BBox box, Set<PrimitiveId> visited) {
        box.add(getBBox());
    }

    @Override
    public void updatePosition() {
        bbox = new BBox(this);
    }

    /**
     * Replies true if this way has incomplete nodes, false otherwise.
     * @return true if this way has incomplete nodes, false otherwise.
     * @since 2587
     */
    public boolean hasIncompleteNodes() {
        Node[] nodes = this.nodes;
        for (Node node : nodes) {
            if (node.isIncomplete())
                return true;
        }
        return false;
    }

    /**
     * Replies true if all nodes of the way have known lat/lon, false otherwise.
     * @return true if all nodes of the way have known lat/lon, false otherwise
     * @since 13033
     */
    public boolean hasOnlyLocatableNodes() {
        Node[] nodes = this.nodes;
        for (Node node : nodes) {
            if (!node.isLatLonKnown())
                return false;
        }
        return true;
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && !hasIncompleteNodes();
    }

    @Override
    public boolean isDrawable() {
        return super.isDrawable() && hasOnlyLocatableNodes();
    }

    /**
     * Replies the length of the way, in metres, as computed by {@link LatLon#greatCircleDistance}.
     * @return The length of the way, in metres
     * @since 4138
     */
    public double getLength() {
        double length = 0;
        Node lastN = null;
        for (Node n:nodes) {
            if (lastN != null) {
                LatLon lastNcoor = lastN.getCoor();
                LatLon coor = n.getCoor();
                if (lastNcoor != null && coor != null) {
                    length += coor.greatCircleDistance(lastNcoor);
                }
            }
            lastN = n;
        }
        return length;
    }

    /**
     * Replies the length of the longest segment of the way, in metres, as computed by {@link LatLon#greatCircleDistance}.
     * @return The length of the segment, in metres
     * @since 8320
     */
    public double getLongestSegmentLength() {
        double length = 0;
        Node lastN = null;
        for (Node n:nodes) {
            if (lastN != null) {
                LatLon lastNcoor = lastN.getCoor();
                LatLon coor = n.getCoor();
                if (lastNcoor != null && coor != null) {
                    double l = coor.greatCircleDistance(lastNcoor);
                    if (l > length) {
                        length = l;
                    }
                }
            }
            lastN = n;
        }
        return length;
    }

    /**
     * Tests if this way is a oneway.
     * @return {@code 1} if the way is a oneway,
     *         {@code -1} if the way is a reversed oneway,
     *         {@code 0} otherwise.
     * @since 5199
     */
    public int isOneway() {
        String oneway = get("oneway");
        if (oneway != null) {
            if ("-1".equals(oneway)) {
                return -1;
            } else {
                Boolean isOneway = OsmUtils.getOsmBoolean(oneway);
                if (isOneway != null && isOneway) {
                    return 1;
                }
            }
        }
        return 0;
    }

    /**
     * Replies the first node of this way, respecting or not its oneway state.
     * @param respectOneway If true and if this way is a reversed oneway, replies the last node. Otherwise, replies the first node.
     * @return the first node of this way, according to {@code respectOneway} and its oneway state.
     * @since 5199
     */
    public Node firstNode(boolean respectOneway) {
        return !respectOneway || isOneway() != -1 ? firstNode() : lastNode();
    }

    /**
     * Replies the last node of this way, respecting or not its oneway state.
     * @param respectOneway If true and if this way is a reversed oneway, replies the first node. Otherwise, replies the last node.
     * @return the last node of this way, according to {@code respectOneway} and its oneway state.
     * @since 5199
     */
    public Node lastNode(boolean respectOneway) {
        return !respectOneway || isOneway() != -1 ? lastNode() : firstNode();
    }

    @Override
    public boolean concernsArea() {
        return hasAreaTags();
    }

    @Override
    public boolean isOutsideDownloadArea() {
        for (final Node n : nodes) {
            if (n.isOutsideDownloadArea()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void keysChangedImpl(Map<String, String> originalKeys) {
        super.keysChangedImpl(originalKeys);
        clearCachedNodeStyles();
    }

    /**
     * Clears all cached styles for all nodes of this way. This should not be called from outside.
     * @see Node#clearCachedStyle()
     */
    public void clearCachedNodeStyles() {
        for (final Node n : nodes) {
            n.clearCachedStyle();
        }
    }

    /**
     * Returns angles of vertices.
     * @return angles of the way
     * @since 13670
     */
    public synchronized List<Pair<Double, Node>> getAngles() {
        List<Pair<Double, Node>> angles = new ArrayList<>();

        for (int i = 1; i < nodes.length - 1; i++) {
            Node n0 = nodes[i - 1];
            Node n1 = nodes[i];
            Node n2 = nodes[i + 1];

            double angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(
                    n0.getEastNorth(), n1.getEastNorth(), n2.getEastNorth()));
            angles.add(new Pair<>(angle, n1));
        }

        angles.add(new Pair<>(Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(
                nodes[nodes.length - 2].getEastNorth(),
                nodes[0].getEastNorth(),
                nodes[1].getEastNorth())), nodes[0]));

        return angles;
    }
}
