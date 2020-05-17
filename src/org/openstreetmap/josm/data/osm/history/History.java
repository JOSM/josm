// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Represents the history of an OSM primitive. The history consists
 * of a list of object snapshots with a specific version.
 * @since 1670
 */
public class History {

    @FunctionalInterface
    private interface FilterPredicate {
        boolean matches(HistoryOsmPrimitive primitive);
    }

    private static History filter(History history, FilterPredicate predicate) {
        List<HistoryOsmPrimitive> out = history.versions.stream()
                .filter(predicate::matches)
                .collect(Collectors.toList());
        return new History(history.id, history.type, out);
    }

    /** the list of object snapshots */
    private final List<HistoryOsmPrimitive> versions;
    /** the object id */
    private final long id;
    /** the object type */
    private final OsmPrimitiveType type;

    /**
     * Creates a new history for an OSM primitive.
     *
     * @param id the id. &gt; 0 required.
     * @param type the primitive type. Must not be null.
     * @param versions a list of versions. Can be null.
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if type is null
     */
    protected History(long id, OsmPrimitiveType type, List<HistoryOsmPrimitive> versions) {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        this.id = id;
        this.type = type;
        this.versions = new ArrayList<>();
        if (versions != null) {
            this.versions.addAll(versions);
        }
    }

    /**
     * Returns a new copy of this history, sorted in ascending order.
     * @return a new copy of this history, sorted in ascending order
     */
    public History sortAscending() {
        List<HistoryOsmPrimitive> copy = new ArrayList<>(versions);
        copy.sort(Comparator.naturalOrder());
        return new History(id, type, copy);
    }

    /**
     * Returns a new copy of this history, sorted in descending order.
     * @return a new copy of this history, sorted in descending order
     */
    public History sortDescending() {
        List<HistoryOsmPrimitive> copy = new ArrayList<>(versions);
        copy.sort(Comparator.reverseOrder());
        return new History(id, type, copy);
    }

    /**
     * Returns a new partial copy of this history, from the given date
     * @param fromDate the starting date
     * @return a new partial copy of this history, from the given date
     */
    public History from(final Date fromDate) {
        return filter(this, primitive -> primitive.getTimestamp().compareTo(fromDate) >= 0);
    }

    /**
     * Returns a new partial copy of this history, until the given date
     * @param untilDate the end date
     * @return a new partial copy of this history, until the given date
     */
    public History until(final Date untilDate) {
        return filter(this, primitive -> primitive.getTimestamp().compareTo(untilDate) <= 0);
    }

    /**
     * Returns a new partial copy of this history, between the given dates
     * @param fromDate the starting date
     * @param untilDate the end date
     * @return a new partial copy of this history, between the given dates
     */
    public History between(Date fromDate, Date untilDate) {
        return this.from(fromDate).until(untilDate);
    }

    /**
     * Returns a new partial copy of this history, from the given version number
     * @param fromVersion the starting version number
     * @return a new partial copy of this history, from the given version number
     */
    public History from(final long fromVersion) {
        return filter(this, primitive -> primitive.getVersion() >= fromVersion);
    }

    /**
     * Returns a new partial copy of this history, to the given version number
     * @param untilVersion the ending version number
     * @return a new partial copy of this history, to the given version number
     */
    public History until(final long untilVersion) {
        return filter(this, primitive -> primitive.getVersion() <= untilVersion);
    }

    /**
     * Returns a new partial copy of this history, betwwen the given version numbers
     * @param fromVersion the starting version number
     * @param untilVersion the ending version number
     * @return a new partial copy of this history, between the given version numbers
     */
    public History between(long fromVersion, long untilVersion) {
        return this.from(fromVersion).until(untilVersion);
    }

    /**
     * Returns a new partial copy of this history, for the given user id
     * @param uid the user id
     * @return a new partial copy of this history, for the given user id
     */
    public History forUserId(final long uid) {
        return filter(this, primitive -> primitive.getUser() != null && primitive.getUser().getId() == uid);
    }

    /**
     * Replies the primitive id for this history.
     *
     * @return the primitive id
     * @see #getPrimitiveId
     * @see #getType
     */
    public long getId() {
        return id;
    }

    /**
     * Replies the primitive id for this history.
     *
     * @return the primitive id
     * @see #getId
     */
    public PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(id, type);
    }

    /**
     * Determines if this history contains a specific version number.
     * @param version the version number to look for
     * @return {@code true} if this history contains {@code version}, {@code false} otherwise
     */
    public boolean contains(long version) {
        return versions.stream().anyMatch(primitive -> primitive.matches(id, version));
    }

    /**
     * Replies the history primitive with version <code>version</code>. null,
     * if no such primitive exists.
     *
     * @param version the version
     * @return the history primitive with version <code>version</code>
     */
    public HistoryOsmPrimitive getByVersion(long version) {
        return versions.stream()
                .filter(primitive -> primitive.matches(id, version))
                .findFirst().orElse(null);
    }

    /**
     * Replies the history primitive at given <code>date</code>. null,
     * if no such primitive exists.
     *
     * @param date the date
     * @return the history primitive at given <code>date</code>
     */
    public HistoryOsmPrimitive getByDate(Date date) {
        History h = sortAscending();

        if (h.versions.isEmpty())
            return null;
        if (h.get(0).getTimestamp().compareTo(date) > 0)
            return null;
        for (int i = 1; i < h.versions.size(); i++) {
            if (h.get(i-1).getTimestamp().compareTo(date) <= 0
                    && h.get(i).getTimestamp().compareTo(date) >= 0)
                return h.get(i);
        }
        return h.getLatest();
    }

    /**
     * Replies the history primitive at index <code>idx</code>.
     *
     * @param idx the index
     * @return the history primitive at index <code>idx</code>
     * @throws IndexOutOfBoundsException if index out or range
     */
    public HistoryOsmPrimitive get(int idx) {
        if (idx < 0 || idx >= versions.size())
            throw new IndexOutOfBoundsException(MessageFormat.format(
                    "Parameter ''{0}'' in range 0..{1} expected. Got ''{2}''.", "idx", versions.size()-1, idx));
        return versions.get(idx);
    }

    /**
     * Replies the earliest entry of this history.
     * @return the earliest entry of this history
     */
    public HistoryOsmPrimitive getEarliest() {
        if (isEmpty())
            return null;
        return sortAscending().versions.get(0);
    }

    /**
     * Replies the latest entry of this history.
     * @return the latest entry of this history
     */
    public HistoryOsmPrimitive getLatest() {
        if (isEmpty())
            return null;
        return sortDescending().versions.get(0);
    }

    /**
     * Replies the number of versions.
     * @return the number of versions
     */
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

    /**
     * Replies the primitive type for this history.
     * @return the primitive type
     * @see #getId
     */
    public OsmPrimitiveType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("History ["
                + (type != null ? ("type=" + type + ", ") : "") + "id=" + id);
        if (versions != null) {
            result.append(", versions=\n");
            for (HistoryOsmPrimitive v : versions) {
                result.append('\t').append(v).append(",\n");
            }
        }
        result.append(']');
        return result.toString();
    }
}
