// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class TagsChangedEvent extends AbstractDatasetChangedEvent {

    private final OsmPrimitive primitive;
    private final Map<String, String> originalKeys;

    /**
     * Constructs a new {@code TagsChangedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param primitive the primitive affected by the change
     * @param originalKeys the original tags
     */
    public TagsChangedEvent(DataSet dataSet, OsmPrimitive primitive, Map<String, String> originalKeys) {
        super(dataSet);
        this.primitive = primitive;
        this.originalKeys = originalKeys;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.tagsChanged(this);
    }

    /**
     * Returns the primitive affected by the change.
     * @return the primitive affected by the change
     */
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

    /**
     * Returns the original tags.
     * @return the original tags
     */
    public Map<String, String> getOriginalKeys() {
        return originalKeys;
    }

}
