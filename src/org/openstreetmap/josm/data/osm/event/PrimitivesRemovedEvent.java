// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PrimitivesRemovedEvent extends AbstractDatasetChangedEvent {

    private final List<? extends OsmPrimitive> primitives;
    private final boolean wasComplete;

    public PrimitivesRemovedEvent(DataSet dataSet, Collection<? extends OsmPrimitive> primitives, boolean wasComplete) {
        super(dataSet);
        this.primitives = Collections.unmodifiableList(new ArrayList<OsmPrimitive>(primitives));
        this.wasComplete = wasComplete;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.primitivesRemoved(this);
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return primitives;
    }

    /**
     *
     * @return True if primitive wasn't really removed from the dataset, it only become incomplete again
     */
    public boolean wasComplete() {
        return wasComplete;
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.PRIMITIVES_REMOVED;
    }

}
