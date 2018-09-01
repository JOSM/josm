// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A ChangesetDataSet holds the content of a changeset.
 */
public class ChangesetDataSet {

    /**
     * Type of primitive modification.
     */
    public enum ChangesetModificationType {
        /** The primitive has been created */
        CREATED,
        /** The primitive has been updated */
        UPDATED,
        /** The primitive has been deleted */
        DELETED
    }

    /**
     * An entry in the changeset dataset.
     */
    public interface ChangesetDataSetEntry {

        /**
         * Returns the type of modification.
         * @return the type of modification
         */
        ChangesetModificationType getModificationType();

        /**
         * Returns the affected history primitive.
         * @return the affected history primitive
         */
        HistoryOsmPrimitive getPrimitive();
    }

    private final Map<PrimitiveId, HistoryOsmPrimitive> primitives = new HashMap<>();
    private final Map<PrimitiveId, ChangesetModificationType> modificationTypes = new HashMap<>();

    /**
     * Remembers a history primitive with the given modification type
     *
     * @param primitive the primitive. Must not be null.
     * @param cmt the modification type. Must not be null.
     * @throws IllegalArgumentException if primitive is null
     * @throws IllegalArgumentException if cmt is null
     */
    public void put(HistoryOsmPrimitive primitive, ChangesetModificationType cmt) {
        CheckParameterUtil.ensureParameterNotNull(primitive, "primitive");
        CheckParameterUtil.ensureParameterNotNull(cmt, "cmt");
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
        return ChangesetModificationType.CREATED == getModificationType(id);
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
        return ChangesetModificationType.UPDATED == getModificationType(id);
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
        return ChangesetModificationType.DELETED == getModificationType(id);
    }

    /**
     * Replies the set of primitives with a specific modification type
     *
     * @param cmt the modification type. Must not be null.
     * @return the set of primitives
     * @throws IllegalArgumentException if cmt is null
     */
    public Set<HistoryOsmPrimitive> getPrimitivesByModificationType(ChangesetModificationType cmt) {
        CheckParameterUtil.ensureParameterNotNull(cmt, "cmt");
        return modificationTypes.entrySet().stream()
                .filter(entry -> entry.getValue() == cmt)
                .map(entry -> primitives.get(entry.getKey()))
                .collect(Collectors.toSet());
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
     * Replies the {@link HistoryOsmPrimitive} with id <code>id</code> from this dataset.
     * null, if there is no such primitive in the data set.
     *
     * @param id the id
     * @return the {@link HistoryOsmPrimitive} with id <code>id</code> from this dataset
     */
    public HistoryOsmPrimitive getPrimitive(PrimitiveId id) {
        if (id == null) return null;
        return primitives.get(id);
    }

    /**
     * Returns an iterator over dataset entries.
     * @return an iterator over dataset entries
     */
    public Iterator<ChangesetDataSetEntry> iterator() {
        return new DefaultIterator();
    }

    private static class DefaultChangesetDataSetEntry implements ChangesetDataSetEntry {
        private final ChangesetModificationType modificationType;
        private final HistoryOsmPrimitive primitive;

        DefaultChangesetDataSetEntry(ChangesetModificationType modificationType, HistoryOsmPrimitive primitive) {
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
        private final Iterator<Entry<PrimitiveId, ChangesetModificationType>> typeIterator;

        DefaultIterator() {
            typeIterator = modificationTypes.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return typeIterator.hasNext();
        }

        @Override
        public ChangesetDataSetEntry next() {
            Entry<PrimitiveId, ChangesetModificationType> next = typeIterator.next();
            return new DefaultChangesetDataSetEntry(next.getValue(), primitives.get(next.getKey()));
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
