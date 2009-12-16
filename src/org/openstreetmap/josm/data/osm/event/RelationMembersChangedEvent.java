// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;

public class RelationMembersChangedEvent extends AbstractDatasetChangedEvent {

    private final Relation relation;

    public RelationMembersChangedEvent(DataSet dataSet, Relation relation) {
        super(dataSet);
        this.relation = relation;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.relationMembersChanged(this);
    }

    public Relation getRelation() {
        return relation;
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return Collections.singletonList(relation);
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.RELATION_MEMBERS_CHANGED;
    }

}
