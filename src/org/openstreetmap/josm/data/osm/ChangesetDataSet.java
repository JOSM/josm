// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A ChangesetDataSet holds the content of a changeset.
 */
public class ChangesetDataSet {

    public static enum ChangesetModificationType {
        CREATED,
        UPDATED,
        DELETED
    }

    public static interface ChangesetDataSetEntry {
        public ChangesetModificationType getModificationType();
        public HistoryOsmPrimitive getPrimitive();
    }

    final private Map<PrimitiveId, HistoryOsmPrimitive> primitives = new HashMap<PrimitiveId, HistoryOsmPrimitive>();
    final private Map<PrimitiveId, ChangesetModificationType> modificationTypes = new HashMap<PrimitiveId, ChangesetModificationType>();

    /**
     * Remembers a history primitive with the given modification type
     *
     * @param primitive the primitive. Must not be null.
     * @param cmt the modification type. Must not be null.
     * @throws IllegalArgumentException thrown if primitive is null
     * @throws IllegalArgumentException thrown if cmt is null
     */
    public void put(HistoryOsmPrimitive primitive, ChangesetModificationType cmt) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(primitive,"primitive");
        CheckParameterUtil.ensureParameterNotNull(cmt,"cmt");
        primitives.put(primitive.getPrimitiveId(), primitive);
        modificationTypes.put(primitive.getPrimitiveId(), cmt);
    }

    /**
     * Replies true if the changeset content contains the object with primitive <code>id</code>.
     * @param id the id.
     * @return true if the changeset content contains the object with primitive <code>id</code>
     */
    public boolean contains(PrimitiveId id) {
        if (id == null) return false;
        return primitives.containsKey(id);
    }

    /**
     * Replies the modification type for the object with id <code>id</code>. Replies null, if id is null or
     * if the object with id <code>id</code> isn't in the changeset content.
     *
     * @param id the id
     * @return the modification type
     */
    public ChangesetModificationType getModificationType(PrimitiveId id) {
        if (!contains(id)) return null;
        return modificationTypes.get(id);
    }

    /**
     * Replies true if the primitive with id <code>id</code> was created in this
     * changeset. Replies false, if id is null.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was created in this
     * changeset.
     */
    public boolean isCreated(PrimitiveId id) {
        if (!contains(id)) return false;
        return ChangesetModificationType.CREATED.equals(getModificationType(id));
    }

    /**
     * Replies true if the primitive with id <code>id</code> was updated in this
     * changeset. Replies false, if id is null.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was updated in this
     * changeset.
     */
    public boolean isUpdated(PrimitiveId id) {
        if (!contains(id)) return false;
        return ChangesetModificationType.UPDATED.equals(getModificationType(id));
    }

    /**
     * Replies true if the primitive with id <code>id</code> was deleted in this
     * changeset. Replies false, if id is null.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was deleted in this
     * changeset.
     */
    public boolean isDeleted(PrimitiveId id) {
        if (!contains(id)) return false;
        return ChangesetModificationType.DELETED.equals(getModificationType(id));
    }

    /**
     * Replies the set of primitives with a specific modification type
     *
     * @param cmt the modification type. Must not be null.
     * @return the set of primitives
     * @throws IllegalArgumentException thrown if cmt is null
     */
    public Set<HistoryOsmPrimitive> getPrimitivesByModificationType(ChangesetModificationType cmt) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(cmt,"cmt");
        HashSet<HistoryOsmPrimitive> ret = new HashSet<HistoryOsmPrimitive>();
        for (Entry<PrimitiveId, ChangesetModificationType> entry: modificationTypes.entrySet()) {
            if (entry.getValue().equals(cmt)) {
                ret.add(primitives.get(entry.getKey()));
            }
        }
        return ret;
    }

    /**
     * Replies the number of objects in the dataset
     *
     * @return the number of objects in the dataset
     */
    public int size() {
        return primitives.size();
    }

    /**
     * Replies the {@link HistoryOsmPrimitive} with id <code>id</code> from this
     * dataset. null, if there is no such primitive in the data set.
     *
     * @param id the id
     * @return  the {@link HistoryOsmPrimitive} with id <code>id</code> from this
     * dataset
     */
    public HistoryOsmPrimitive getPrimitive(PrimitiveId id) {
        if (id == null)  return null;
        return primitives.get(id);
    }

    public Iterator<ChangesetDataSetEntry> iterator() {
        return new DefaultIterator();
    }

    private static class DefaultChangesetDataSetEntry implements ChangesetDataSetEntry {
        private ChangesetModificationType modificationType;
        private HistoryOsmPrimitive primitive;

        public DefaultChangesetDataSetEntry(ChangesetModificationType modificationType, HistoryOsmPrimitive primitive) {
            this.modificationType = modificationType;
            this.primitive = primitive;
        }

        @Override
        public ChangesetModificationType getModificationType() {
            return modificationType;
        }

        @Override
        public HistoryOsmPrimitive getPrimitive() {
            return primitive;
        }
    }

    private class DefaultIterator implements Iterator<ChangesetDataSetEntry> {
        private Iterator<Entry<PrimitiveId, ChangesetModificationType>> typeIterator;

        public DefaultIterator() {
            typeIterator = modificationTypes.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return typeIterator.hasNext();
        }

        @Override
        public ChangesetDataSetEntry next() {
            Entry<PrimitiveId, ChangesetModificationType> next = typeIterator.next();
            ChangesetModificationType type = next.getValue();
            HistoryOsmPrimitive primitive = primitives.get(next.getKey());
            return new DefaultChangesetDataSetEntry(type, primitive);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
