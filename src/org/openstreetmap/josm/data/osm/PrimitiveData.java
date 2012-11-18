// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class can be used to save properties of OsmPrimitive.
 *
 * The main difference between PrimitiveData
 * and OsmPrimitive is that PrimitiveData is not part of the dataset and changes in PrimitiveData are not
 * reported by events
 */
public abstract class PrimitiveData extends AbstractPrimitive {

    public PrimitiveData() {
        id = OsmPrimitive.generateUniqueId();
    }

    public PrimitiveData(PrimitiveData data) {
        cloneFrom(data);
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * override to make it public
     */
    @Override
    public void setIncomplete(boolean incomplete) {
        super.setIncomplete(incomplete);
    }

    public abstract PrimitiveData makeCopy();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id).append(Arrays.toString(keys)).append(getFlagsAsString());
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    static public <T extends PrimitiveData> List<T> getFilteredList(Collection<T> list, OsmPrimitiveType type) {
        List<T> ret = new ArrayList<T>();
        for(PrimitiveData p: list) {
            if (type.getDataClass().isInstance(p)) {
                ret.add((T)p);
            }
        }
        return ret;
    }

    @Override
    protected final void keysChangedImpl(Map<String, String> originalKeys) {
    }

    @Override
    public abstract OsmPrimitiveType getType();
}
