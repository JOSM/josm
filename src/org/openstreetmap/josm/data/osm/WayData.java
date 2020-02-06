// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

/**
 * The data (tags and node ids) that is stored for a way in the database.
 * @since 2284
 */
public class WayData extends PrimitiveData implements IWay<NodeData> {

    private static final long serialVersionUID = 106944939313286415L;
    private static final UniqueIdGenerator idGenerator = Way.idGenerator;
    private List<Long> nodes = new ArrayList<>();

    /**
     * Constructs a new {@code NodeData}.
     */
    public WayData() {
        // contents can be set later with setters
        this(idGenerator.generateUniqueId());
    }

    /**
     * Constructs a new {@code WayData} with given id.
     * @param id id
     * @since 12017
     */
    public WayData(long id) {
        super(id);
    }

    /**
     * Constructs a new {@code WayData}.
     * @param data way data to copy
     */
    public WayData(WayData data) {
        super(data);
        nodes.addAll(data.getNodeIds());
    }

    @Override
    public List<NodeData> getNodes() {
        throw new UnsupportedOperationException("Use getNodeIds() instead");
    }

    @Override
    public NodeData getNode(int index) {
        throw new UnsupportedOperationException("Use getNodeId(int) instead");
    }

    @Override
    public List<Long> getNodeIds() {
        return nodes;
    }

    @Override
    public int getNodesCount() {
        return nodes.size();
    }

    @Override
    public long getNodeId(int idx) {
        return nodes.get(idx);
    }

    @Override
    public boolean isClosed() {
        if (isIncomplete()) return false;
        return nodes.get(0).equals(nodes.get(nodes.size() - 1));
    }

    @Override
    public void setNodes(List<NodeData> nodes) {
        throw new UnsupportedOperationException("Use setNodeIds(List) instead");
    }

    /**
     * Sets the nodes array
     * @param nodes The nodes this way consists of
     * @since 13907
     */
    public void setNodeIds(List<Long> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }

    @Override
    public WayData makeCopy() {
        return new WayData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " WAY" + nodes;
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BBox getBBox() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeData firstNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeData lastNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFirstLastNode(INode n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInnerNode(INode n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniqueIdGenerator getIdGenerator() {
        return idGenerator;
    }
}
