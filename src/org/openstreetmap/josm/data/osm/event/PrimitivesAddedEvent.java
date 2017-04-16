// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PrimitivesAddedEvent extends AbstractDatasetChangedEvent {

    private final List<? extends OsmPrimitive> primitives;
    private final boolean wasIncomplete;

    /**
     * Constructs a new {@code PrimitivesAddedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param primitives the list of primitives affected by the change
     * @param wasIncomplete {@code true} if primitive was in dataset before (so it's not really added), but it was incomplete
     */
    public PrimitivesAddedEvent(DataSet dataSet, Collection<? extends OsmPrimitive> primitives, boolean wasIncomplete) {
        super(dataSet);
        this.primitives = Collections.unmodifiableList(new ArrayList<>(primitives));
        this.wasIncomplete = wasIncomplete;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.primitivesAdded(this);
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return primitives;
    }

    /**
     * Determines if primitive was in dataset before (so it's not really added), but it was incomplete
     * @return {@code true} if primitive was in dataset before (so it's not really added), but it was incomplete
     */
    public boolean wasIncomplete() {
        return wasIncomplete;
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.PRIMITIVES_ADDED;
    }

}
