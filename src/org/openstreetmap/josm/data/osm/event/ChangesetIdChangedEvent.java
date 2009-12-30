// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class ChangesetIdChangedEvent extends AbstractDatasetChangedEvent {

    private final List<OsmPrimitive> primitives;
    private final int oldChangesetId;
    private final int newChangesetId;

    public ChangesetIdChangedEvent(DataSet dataSet, List<OsmPrimitive> primitives, int oldChangesetId, int newChangesetId) {
        super(dataSet);
        this.primitives = primitives;
        this.oldChangesetId = oldChangesetId;
        this.newChangesetId = newChangesetId;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.otherDatasetChange(this);
    }

    @Override
    public List<OsmPrimitive> getPrimitives() {
        return primitives;
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.CHANGESET_ID_CHANGED;
    }

    public int getOldChangesetId() {
        return oldChangesetId;
    }

    public int getNewChangesetId() {
        return newChangesetId;
    }

}
