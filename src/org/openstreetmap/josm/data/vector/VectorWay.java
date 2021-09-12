// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.UniqueIdGenerator;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

/**
 * The "Way" type for a Vector layer
 *
 * @author Taylor Smock
 * @since 17862
 */
public class VectorWay extends VectorPrimitive implements IWay<VectorNode> {
    private static final UniqueIdGenerator WAY_GENERATOR = new UniqueIdGenerator();
    private final List<VectorNode> nodes = new ArrayList<>();
    private BBox cachedBBox;

    /**
     * Create a new way for a layer
     * @param layer The layer for the way
     */
    public VectorWay(String layer) {
        super(layer);
    }

    @Override
    public UniqueIdGenerator getIdGenerator() {
        return WAY_GENERATOR;
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BBox getBBox() {
        if (this.cachedBBox == null) {
            BBox tBBox = new BBox();
            for (INode node : this.getNodes()) {
                tBBox.add(node.getBBox());
            }
            this.cachedBBox = tBBox.toImmutable();
        }
        return this.cachedBBox;
    }

    @Override
    public int getNodesCount() {
        return this.getNodes().size();
    }

    @Override
    public VectorNode getNode(int index) {
        return this.getNodes().get(index);
    }

    @Override
    public List<VectorNode> getNodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    @Override
    public void setNodes(List<VectorNode> nodes) {
        this.nodes.forEach(node -> node.removeReferrer(this));
        this.nodes.clear();
        if (nodes != null) {
            nodes.forEach(node -> node.addReferrer(this));
            this.nodes.addAll(nodes);
        }
        this.cachedBBox = null;
    }

    @Override
    public List<Long> getNodeIds() {
        return this.getNodes().stream().map(VectorNode::getId).collect(Collectors.toList());
    }

    @Override
    public long getNodeId(int idx) {
        return this.getNodes().get(idx).getId();
    }

    @Override
    public boolean isClosed() {
        return this.firstNode() != null && this.firstNode().equals(this.lastNode());
    }

    @Override
    public VectorNode firstNode() {
        if (this.nodes.isEmpty()) {
            return null;
        }
        return this.getNode(0);
    }

    @Override
    public VectorNode lastNode() {
        if (this.nodes.isEmpty()) {
            return null;
        }
        return this.getNode(this.getNodesCount() - 1);
    }

    @Override
    public boolean isFirstLastNode(INode n) {
        if (this.nodes.isEmpty()) {
            return false;
        }
        return this.firstNode().equals(n) || this.lastNode().equals(n);
    }

    @Override
    public boolean isInnerNode(INode n) {
        if (this.nodes.isEmpty()) {
            return false;
        }
        return !this.firstNode().equals(n) && !this.lastNode().equals(n) && this.nodes.stream()
          .anyMatch(vectorNode -> vectorNode.equals(n));
    }

    @Override
    public OsmPrimitiveType getType() {
        return this.isClosed() ? OsmPrimitiveType.CLOSEDWAY : OsmPrimitiveType.WAY;
    }
}
