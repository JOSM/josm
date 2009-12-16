// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class NodeMovedEvent extends AbstractDatasetChangedEvent {

    private final Node node;

    public NodeMovedEvent(DataSet dataSet, Node node) {
        super(dataSet);
        this.node = node;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.nodeMoved(this);
    }

    public Node getNode() {
        return node;
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return Collections.singletonList(node);
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.NODE_MOVED;
    }

}
