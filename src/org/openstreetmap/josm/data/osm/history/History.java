// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents the history of an OSM primitive. The history consists
 * of a list of object snapshots with a specific version.
 *
 */
public class History{
    private static interface FilterPredicate {
        boolean matches(HistoryOsmPrimitive primitive);
    }

    private static History filter(History history, FilterPredicate predicate) {
        List<HistoryOsmPrimitive> out = new ArrayList<HistoryOsmPrimitive>();
        for (HistoryOsmPrimitive primitive: history.versions) {
            if (predicate.matches(primitive)) {
                out.add(primitive);
            }
        }
        return new History(history.id, history.type,out);
    }

    /** the list of object snapshots */
    private List<HistoryOsmPrimitive> versions;
    /** the object id */
    private final long id;
    private final OsmPrimitiveType type;

    /**
     * Creates a new history for an OSM primitive
     *
     * @param id the id. &gt; 0 required.
     * @param type the primitive type. Must not be null.
     * @param versions a list of versions. Can be null.
     * @throws IllegalArgumentException thrown if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     *
     */
    protected History(long id, OsmPrimitiveType type, List<HistoryOsmPrimitive> versions) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        this.id = id;
        this.type = type;
        this.versions = new ArrayList<HistoryOsmPrimitive>();
        if (versions != null) {
            this.versions.addAll(versions);
        }
    }

    public History sortAscending() {
        List<HistoryOsmPrimitive> copy = new ArrayList<HistoryOsmPrimitive>(versions);
        Collections.sort(
                copy,
                new Comparator<HistoryOsmPrimitive>() {
                    @Override
                    public int compare(HistoryOsmPrimitive o1, HistoryOsmPrimitive o2) {
                        return o1.compareTo(o2);
                    }
                }
                );
        return new History(id, type, copy);
    }

    public History sortDescending() {
        List<HistoryOsmPrimitive> copy = new ArrayList<HistoryOsmPrimitive>(versions);
        Collections.sort(
                copy,
                new Comparator<HistoryOsmPrimitive>() {
                    @Override
                    public int compare(HistoryOsmPrimitive o1, HistoryOsmPrimitive o2) {
                        return o2.compareTo(o1);
                    }
                }
                );
        return new History(id, type,copy);
    }

    public History from(final Date fromDate) {
        return filter(
                this,
                new FilterPredicate() {
                    @Override
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getTimestamp().compareTo(fromDate) >= 0;
                    }
                }
                );
    }

    public History until(final Date untilDate) {
        return filter(
                this,
                new FilterPredicate() {
                    @Override
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getTimestamp().compareTo(untilDate) <= 0;
                    }
                }
                );
    }

    public History between(Date fromDate, Date untilDate) {
        return this.from(fromDate).until(untilDate);
    }

    public History from(final long fromVersion) {
        return filter(
                this,
                new FilterPredicate() {
                    @Override
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getVersion() >= fromVersion;
                    }
                }
                );
    }

    public History until(final long untilVersion) {
        return filter(
                this,
                new FilterPredicate() {
                    @Override
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getVersion() <= untilVersion;
                    }
                }
                );
    }

    public History between(long fromVersion, long untilVersion) {
        return this.from(fromVersion).until(untilVersion);
    }

    public History forUserId(final long uid) {
        return filter(
                this,
                new FilterPredicate() {
                    @Override
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getUser() != null && primitive.getUser().getId() == uid;
                    }
                }
                );
    }

    public long getId() {
        return id;
    }

    /**
     * Replies the primitive id for this history.
     *
     * @return the primitive id
     */
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(id, type);
    }

    public boolean contains(long version){
        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id,version))
                return true;
        }
        return false;
    }

    /**
     * Replies the history primitive with version <code>version</code>. null,
     * if no such primitive exists.
     *
     * @param version the version
     * @return the history primitive with version <code>version</code>
     */
    public HistoryOsmPrimitive getByVersion(long version) {
        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id,version))
                return primitive;
        }
        return null;
    }

    public HistoryOsmPrimitive getByDate(Date date) {
        History h = sortAscending();

        if (h.versions.isEmpty())
            return null;
        if (h.get(0).getTimestamp().compareTo(date)> 0)
            return null;
        for (int i = 1; i < h.versions.size();i++) {
            if (h.get(i-1).getTimestamp().compareTo(date) <= 0
                    && h.get(i).getTimestamp().compareTo(date) >= 0)
                return h.get(i);
        }
        return h.getLatest();
    }

    public HistoryOsmPrimitive get(int idx) throws IndexOutOfBoundsException {
        if (idx < 0 || idx >= versions.size())
            throw new IndexOutOfBoundsException(MessageFormat.format("Parameter ''{0}'' in range 0..{1} expected. Got ''{2}''.", "idx", versions.size()-1, idx));
        return versions.get(idx);
    }

    public HistoryOsmPrimitive getEarliest() {
        if (isEmpty())
            return null;
        return sortAscending().versions.get(0);
    }

    public HistoryOsmPrimitive getLatest() {
        if (isEmpty())
            return null;
        return sortDescending().versions.get(0);
    }

    public int getNumVersions() {
        return versions.size();
    }

    /**
     * Returns true if this history contains no version.
     * @return {@code true} if this history contains no version, {@code false} otherwise
     */
    public final boolean isEmpty() {
        return versions.isEmpty();
    }

    public OsmPrimitiveType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("History ["
                + (type != null ? "type=" + type + ", " : "") + "id=" + id);
        if (versions != null) {
            result.append(", versions=\n");
            for (HistoryOsmPrimitive v : versions) {
                result.append("\t").append(v).append(",\n");
            }
        }
        result.append("]");
        return result.toString();
    }
}
