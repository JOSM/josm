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

    public PrimitivesAddedEvent(DataSet dataSet, Collection<? extends OsmPrimitive> primitives, boolean wasIncomplete) {
        super(dataSet);
        this.primitives = Collections.unmodifiableList(new ArrayList<OsmPrimitive>(primitives));
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
     *
     * @return True if primitive was in dataset before (so it's not really added), but it was incomplete
     */
    public boolean wasIncomplete() {
        return wasIncomplete;
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.PRIMITIVES_ADDED;
    }

}
