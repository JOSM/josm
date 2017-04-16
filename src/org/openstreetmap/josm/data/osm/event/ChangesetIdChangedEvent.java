// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class ChangesetIdChangedEvent extends AbstractDatasetChangedEvent {

    private final List<OsmPrimitive> primitives;
    private final int oldChangesetId;
    private final int newChangesetId;

    /**
     * Constructs a new {@code ChangesetIdChangedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param primitives list of affected primitives
     * @param oldChangesetId old changeset id
     * @param newChangesetId new changeset id
     */
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

    /**
     * Returns the old changeset id.
     * @return the old changeset id
     */
    public int getOldChangesetId() {
        return oldChangesetId;
    }

    /**
     * Returns the new changeset id.
     * @return the new changeset id
     */
    public int getNewChangesetId() {
        return newChangesetId;
    }

}
