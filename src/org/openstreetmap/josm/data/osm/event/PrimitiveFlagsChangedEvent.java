// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Represents a change in {@link OsmPrimitive#flags} unless covered by a more specific {@linkplain AbstractDatasetChangedEvent event}
 */
public class PrimitiveFlagsChangedEvent extends AbstractDatasetChangedEvent {

    private final OsmPrimitive primitive;

    /**
     * Constructs a new {@code PrimitiveFlagsChangedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param primitive the primitive affected by the change
     */
    public PrimitiveFlagsChangedEvent(DataSet dataSet, OsmPrimitive primitive) {
        super(dataSet);
        this.primitive = primitive;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.otherDatasetChange(this);
    }

    @Override
    public Collection<? extends OsmPrimitive> getPrimitives() {
        return Collections.singleton(primitive);
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.PRIMITIVE_FLAGS_CHANGED;
    }
}
