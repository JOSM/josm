// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A data set holding histories of OSM primitives.
 * 
 *
 */
public class HistoryDataSet {

    /** the unique instance */
    private static HistoryDataSet historyDataSet;

    /**
     * Replies the unique instance of the history data set
     * 
     * @return the unique instance of the history data set
     */
    public static HistoryDataSet getInstance() {
        if (historyDataSet == null) {
            historyDataSet = new HistoryDataSet();
        }
        return  historyDataSet;
    }

    /** the history data */
    private HashMap<Long, ArrayList<HistoryOsmPrimitive>> data;
    private CopyOnWriteArrayList<HistoryDataSetListener> listeners;

    public HistoryDataSet() {
        data = new HashMap<Long, ArrayList<HistoryOsmPrimitive>>();
        listeners = new CopyOnWriteArrayList<HistoryDataSetListener>();
    }

    public void addHistoryDataSetListener(HistoryDataSetListener listener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeHistoryDataSetListener(HistoryDataSetListener listener) {
        synchronized(listeners) {
            if (listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }
    }

    protected void fireHistoryUpdated(long id) {
        for (HistoryDataSetListener l : listeners) {
            l.historyUpdated(this, id);
        }
    }

    /**
     * Replies the history primitive for the primitive with id <code>id</code>
     * and version <code>version</code>
     * 
     * @param id the id of the primitive
     * @param version the version of the primitive
     * @return the history primitive for the primitive with id <code>id</code>
     * and version <code>version</code>
     * @throws NoSuchElementException thrown if this dataset doesn't include the respective
     * history primitive
     */
    public HistoryOsmPrimitive get(long id, long version) throws NoSuchElementException{
        ArrayList<HistoryOsmPrimitive> versions = data.get(id);
        if (versions == null)
            throw new NoSuchElementException(tr("Didn't find an primitive with id {0} in this dataset", id));

        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id, version))
                return primitive;
        }
        throw new NoSuchElementException(tr("Didn't find an primitive with id {0} and version {1} in this dataset", id, version));
    }

    /**
     * Adds a history primitive to the data set
     * 
     * @param primitive  the history primitive to add
     */
    public void put(HistoryOsmPrimitive primitive) {
        if (data.get(primitive.getId()) == null) {
            data.put(primitive.getId(), new ArrayList<HistoryOsmPrimitive>());
        }
        data.get(primitive.getId()).add(primitive);
        fireHistoryUpdated(primitive.getId());
    }

    /**
     * Replies the history for a given primitive with id <code>id</code>
     * 
     * @param id the id
     * @return the history
     */
    public History getHistory(long id) {
        ArrayList<HistoryOsmPrimitive> versions = data.get(id);
        if (versions == null)
            return null;
        return new History(id, versions);
    }

    /**
     * merges the histories from the {@see HistoryDataSet} other in this history data set
     * 
     * @param other the other history data set. Ignored if null.
     */
    public void mergeInto(HistoryDataSet other) {
        if (other == null)
            return;
        for (Long id : other.data.keySet()) {
            this.data.put(id, other.data.get(id));
        }
        fireHistoryUpdated(0);
    }
}
