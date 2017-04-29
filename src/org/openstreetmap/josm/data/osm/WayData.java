// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

public class WayData extends PrimitiveData implements IWay {

    private static final long serialVersionUID = 106944939313286415L;
    private List<Long> nodes = new ArrayList<>();

    /**
     * Constructs a new {@code NodeData}.
     */
    public WayData() {
        // contents can be set later with setters
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
        nodes.addAll(data.getNodes());
    }

    public List<Long> getNodes() {
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

    public void setNodes(List<Long> nodes) {
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

}
