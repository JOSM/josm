// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class TagsChangedEvent extends AbstractDatasetChangedEvent {

    private final OsmPrimitive primitive;

    public TagsChangedEvent(DataSet dataSet, OsmPrimitive primitive) {
        super(dataSet);
        this.primitive = primitive;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.tagsChanged(this);
    }

    public OsmPrimitive getPrimitive() {
        return primitive;
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return Collections.singletonList(primitive);
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.TAGS_CHANGED;
    }

}
