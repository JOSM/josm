// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import static org.openstreetmap.josm.tools.I18n.tr;

public class History{
    private static interface FilterPredicate {
        boolean matches(HistoryOsmPrimitive primitive);
    }

    private static History filter(History history, FilterPredicate predicate) {
        ArrayList<HistoryOsmPrimitive> out = new ArrayList<HistoryOsmPrimitive>();
        for (HistoryOsmPrimitive primitive: history.versions) {
            if (predicate.matches(primitive)) {
                out.add(primitive);
            }
        }
        return new History(history.id, out);
    }

    private ArrayList<HistoryOsmPrimitive> versions;
    long id;

    protected History(long id, List<HistoryOsmPrimitive> versions) {
        this.id = id;
        this.versions = new ArrayList<HistoryOsmPrimitive>();
        this.versions.addAll(versions);
    }

    public History sortAscending() {
        ArrayList<HistoryOsmPrimitive> copy = new ArrayList<HistoryOsmPrimitive>(versions);
        Collections.sort(
                copy,
                new Comparator<HistoryOsmPrimitive>() {
                    public int compare(HistoryOsmPrimitive o1, HistoryOsmPrimitive o2) {
                        return o1.compareTo(o2);
                    }
                }
        );
        return new History(id, copy);
    }

    public History sortDescending() {
        ArrayList<HistoryOsmPrimitive> copy = new ArrayList<HistoryOsmPrimitive>(versions);
        Collections.sort(
                copy,
                new Comparator<HistoryOsmPrimitive>() {
                    public int compare(HistoryOsmPrimitive o1, HistoryOsmPrimitive o2) {
                        return o2.compareTo(o1);
                    }
                }
        );
        return new History(id, copy);
    }

    public History from(final Date fromDate) {
        return filter(
                this,
                new FilterPredicate() {
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
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getVersion() <= untilVersion;
                    }
                }
        );
    }

    public History between(long fromVersion, long untilVersion) {
        return this.from(fromVersion).until(untilVersion);
    }

    public History forUser(final String user) {
        return filter(
                this,
                new FilterPredicate() {
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getUser().equals(user);
                    }
                }
        );
    }

    public History forUserId(final long uid) {
        return filter(
                this,
                new FilterPredicate() {
                    public boolean matches(HistoryOsmPrimitive primitive) {
                        return primitive.getUid() == uid;
                    }
                }
        );
    }

    public long getId() {
        return id;
    }

    public boolean contains(long version){
        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id,version))
                return true;
        }
        return false;
    }

    public HistoryOsmPrimitive getByVersion(long version) {
        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id,version))
                return primitive;
        }
        throw new NoSuchElementException(tr("There's no primitive with version {0} in this history", version));
    }

    public HistoryOsmPrimitive getByDate(Date date) {
        sortAscending();

        if (versions.isEmpty())
            throw new NoSuchElementException(tr("There's no version valid at date ''{0}'' in this history", date));
        if (get(0).getTimestamp().compareTo(date)> 0)
            throw new NoSuchElementException(tr("There's no version valid at date ''{0}'' in this history", date));
        for (int i = 1; i < versions.size();i++) {
            if (get(i-1).getTimestamp().compareTo(date) <= 0
                    && get(i).getTimestamp().compareTo(date) >= 0)
                return get(i);
        }
        return getLatest();
    }

    public HistoryOsmPrimitive get(int idx) {
        if (idx < 0 || idx >= versions.size())
            throw new IndexOutOfBoundsException(tr("parameter ''{0}'' in range 0..{1} expected, got {2}", "idx", versions.size()-1, idx));
        return versions.get(idx);
    }

    public HistoryOsmPrimitive getEarliest() {
        if (isEmpty())
            throw new NoSuchElementException(tr("no earliest version found. History is empty."));
        return sortAscending().versions.get(0);
    }

    public HistoryOsmPrimitive getLatest() {
        if (isEmpty())
            throw new NoSuchElementException(tr("no latest version found. History is empty."));
        return sortDescending().versions.get(0);
    }

    public int getNumVersions() {
        return versions.size();
    }

    public boolean isEmpty() {
        return versions.isEmpty();
    }
}
