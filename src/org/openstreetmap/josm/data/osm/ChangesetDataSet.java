// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * A ChangesetDataSet holds the content of a changeset. Typically, a primitive is modified only once in a changeset,
 * but if there are multiple modifications, the first and last are kept. Further intermediate versions are not kept.
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

    /** maps an id to either one {@link ChangesetDataSetEntry} or an array of {@link ChangesetDataSetEntry} */
    private final Map<PrimitiveId, Object> entryMap = new HashMap<>();

    /**
     * Remembers a history primitive with the given modification type
     *
     * @param primitive the primitive. Must not be null.
     * @param cmt the modification type. Must not be null.
     * @throws IllegalArgumentException if primitive is null
     * @throws IllegalArgumentException if cmt is null
     * @throws IllegalArgumentException if the same primitive was already stored with a higher or equal version
     */
    public void put(HistoryOsmPrimitive primitive, ChangesetModificationType cmt) {
        CheckParameterUtil.ensureParameterNotNull(primitive, "primitive");
        CheckParameterUtil.ensureParameterNotNull(cmt, "cmt");
        DefaultChangesetDataSetEntry csEntry = new DefaultChangesetDataSetEntry(cmt, primitive);
        Object val = entryMap.get(primitive.getPrimitiveId());
        ChangesetDataSetEntry[] entries;
        if (val == null) {
            entryMap.put(primitive.getPrimitiveId(), csEntry);
            return;
        }
        if (val instanceof ChangesetDataSetEntry) {
            entries = new ChangesetDataSetEntry[2];
            entries[0] = (ChangesetDataSetEntry) val;
            if (primitive.getVersion() <= entries[0].getPrimitive().getVersion()) {
                throw new IllegalArgumentException(
                        tr("Changeset {0}: Unexpected order of versions for {1}: v{2} is not higher than v{3}",
                                String.valueOf(primitive.getChangesetId()), primitive.getPrimitiveId(),
                                primitive.getVersion(), entries[0].getPrimitive().getVersion()));
            }
        } else {
            entries = (ChangesetDataSetEntry[]) val;
        }
        if (entries[1] != null) {
            Logging.info("Changeset {0}: Change of {1} v{2} is replaced by version v{3}",
                    String.valueOf(primitive.getChangesetId()), primitive.getPrimitiveId(),
                    entries[1].getPrimitive().getVersion(), primitive.getVersion());
        }
        entries[1] = csEntry;
        entryMap.put(primitive.getPrimitiveId(), entries);
    }

    /**
     * Replies true if the changeset content contains the object with primitive <code>id</code>.
     * @param id the id.
     * @return true if the changeset content contains the object with primitive <code>id</code>
     */
    public boolean contains(PrimitiveId id) {
        if (id == null) return false;
        return entryMap.containsKey(id);
    }

    /**
     * Replies the last modification type for the object with id <code>id</code>. Replies null, if id is null or
     * if the object with id <code>id</code> isn't in the changeset content.
     *
     * @param id the id
     * @return the last modification type or null
     */
    public ChangesetModificationType getModificationType(PrimitiveId id) {
        ChangesetDataSetEntry e = getLastEntry(id);
        return e != null ? e.getModificationType() : null;
    }

    /**
     * Replies true if the primitive with id <code>id</code> was created in this
     * changeset. Replies false, if id is null or not in the dataset.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was created in this
     * changeset.
     */
    public boolean isCreated(PrimitiveId id) {
        ChangesetDataSetEntry e = getFirstEntry(id);
        return e != null && e.getModificationType() == ChangesetModificationType.CREATED;
    }

    /**
     * Replies true if the primitive with id <code>id</code> was updated in this
     * changeset. Replies false, if id is null or not in the dataset.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was updated in this
     * changeset.
     */
    public boolean isUpdated(PrimitiveId id) {
        ChangesetDataSetEntry e = getLastEntry(id);
        return e != null && e.getModificationType() == ChangesetModificationType.UPDATED;
    }

    /**
     * Replies true if the primitive with id <code>id</code> was deleted in this
     * changeset. Replies false, if id is null or not in the dataset.
     *
     * @param id the id
     * @return true if the primitive with id <code>id</code> was deleted in this
     * changeset.
     */
    public boolean isDeleted(PrimitiveId id) {
        ChangesetDataSetEntry e = getLastEntry(id);
        return e != null && e.getModificationType() == ChangesetModificationType.DELETED;
    }

    /**
     * Replies the number of primitives in the dataset.
     *
     * @return the number of primitives in the dataset.
     */
    public int size() {
        return entryMap.size();
    }

    /**
     * Replies the {@link HistoryOsmPrimitive} with id <code>id</code> from this dataset.
     * null, if there is no such primitive in the data set. If the primitive was modified
     * multiple times, the last version is returned.
     *
     * @param id the id
     * @return the {@link HistoryOsmPrimitive} with id <code>id</code> from this dataset
     */
    public HistoryOsmPrimitive getPrimitive(PrimitiveId id) {
        ChangesetDataSetEntry e = getLastEntry(id);
        return e != null ? e.getPrimitive() : null;
    }

    /**
     * Returns an unmodifiable set of all primitives in this dataset.
     * @return an unmodifiable set of all primitives in this dataset.
     * @since 14946
     */
    public Set<PrimitiveId> getIds() {
        return Collections.unmodifiableSet(entryMap.keySet());
    }

    /**
     * Replies the first {@link ChangesetDataSetEntry} with id <code>id</code> from this dataset.
     * null, if there is no such primitive in the data set.
     * @param id the id
     * @return the first {@link ChangesetDataSetEntry} with id <code>id</code> from this dataset or null.
     * @since 14946
     */
    public ChangesetDataSetEntry getFirstEntry(PrimitiveId id) {
        return getEntry(id, 0);
    }

    /**
     * Replies the last {@link ChangesetDataSetEntry} with id <code>id</code> from this dataset.
     * null, if there is no such primitive in the data set.
     * @param id the id
     * @return the last {@link ChangesetDataSetEntry} with id <code>id</code> from this dataset or null.
     * @since 14946
     */
    public ChangesetDataSetEntry getLastEntry(PrimitiveId id) {
        return getEntry(id, 1);
    }

    private ChangesetDataSetEntry getEntry(PrimitiveId id, int n) {
        if (id == null)
            return null;
        Object val = entryMap.get(id);
        if (val == null)
            return null;
        if (val instanceof ChangesetDataSetEntry[]) {
            ChangesetDataSetEntry[] entries = (ChangesetDataSetEntry[]) val;
            return entries[n];
        } else {
            return (ChangesetDataSetEntry) val;
        }
    }

    /**
     * Returns an iterator over dataset entries. The elements are returned in no particular order.
     * @return an iterator over dataset entries. If a primitive was changed multiple times, only the last entry is returned.
     */
    public Iterator<ChangesetDataSetEntry> iterator() {
        return new DefaultIterator(entryMap);
    }

    /**
     * Class to keep one entry of a changeset: the combination of modification type and primitive.
     */
    public static class DefaultChangesetDataSetEntry implements ChangesetDataSetEntry {
        private final ChangesetModificationType modificationType;
        private final HistoryOsmPrimitive primitive;

        /**
         * Construct new entry.
         * @param modificationType the modification type
         * @param primitive the primitive
         */
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

        @Override
        public String toString() {
            return modificationType.toString() + " " + primitive.toString();
        }
    }

    private static class DefaultIterator implements Iterator<ChangesetDataSetEntry> {
        private final Iterator<Entry<PrimitiveId, Object>> typeIterator;

        DefaultIterator(Map<PrimitiveId, Object> entryMap) {
            typeIterator = entryMap.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return typeIterator.hasNext();
        }

        @Override
        public ChangesetDataSetEntry next() {
            Entry<PrimitiveId, Object> next = typeIterator.next();
            // get last entry
            Object val = next.getValue();
            ChangesetDataSetEntry last;
            if (val instanceof ChangesetDataSetEntry[]) {
                ChangesetDataSetEntry[] entries = (ChangesetDataSetEntry[]) val;
                last = entries[1];
            } else {
                last = (ChangesetDataSetEntry) val;
            }
            return new DefaultChangesetDataSetEntry(last.getModificationType(), last.getPrimitive());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
