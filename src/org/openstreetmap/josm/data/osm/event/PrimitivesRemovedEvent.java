// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * An event that is triggered when primitives were removed from the dataset
 */
public class PrimitivesRemovedEvent extends AbstractDatasetChangedEvent {

    private final List<? extends OsmPrimitive> primitives;
    private final boolean wasComplete;

    /**
     * Constructs a new {@code PrimitivesRemovedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param primitives the list of primitives affected by the change
     * @param wasComplete {@code true} if primitive wasn't really removed from the dataset, it only become incomplete again
     */
    public PrimitivesRemovedEvent(DataSet dataSet, Collection<? extends OsmPrimitive> primitives, boolean wasComplete) {
        super(dataSet);
        this.primitives = Collections.unmodifiableList(new ArrayList<>(primitives));
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
     * Determines if primitive wasn't really removed from the dataset, it only become incomplete again.
     * @return {@code true} if primitive wasn't really removed from the dataset, it only become incomplete again
     */
    public boolean wasComplete() {
        return wasComplete;
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.PRIMITIVES_REMOVED;
    }

}
