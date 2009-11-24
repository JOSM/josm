// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;

/**
 * A data set holding histories of OSM primitives.
 *
 *
 */
public class HistoryDataSet {
    private final static Logger logger = Logger.getLogger(HistoryDataSet.class.getName());

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
    private HashMap<PrimitiveId, ArrayList<HistoryOsmPrimitive>> data;
    private CopyOnWriteArrayList<HistoryDataSetListener> listeners;

    public HistoryDataSet() {
        data = new HashMap<PrimitiveId, ArrayList<HistoryOsmPrimitive>>();
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

    protected void fireHistoryUpdated(SimplePrimitiveId id) {
        for (HistoryDataSetListener l : listeners) {
            l.historyUpdated(this, id);
        }
    }

    /**
     * Replies the history primitive for the primitive with id <code>id</code>
     * and version <code>version</code>. null, if no such primitive exists.
     *
     * @param id the id of the primitive. > 0 required.
     * @param type the primitive type. Must not be null.
     * @param version the version of the primitive. > 0 required
     * @return the history primitive for the primitive with id <code>id</code>,
     * type <code>type</code>, and version <code>version</code>
     */
    public HistoryOsmPrimitive get(long id, OsmPrimitiveType type, long version){
        if (id <= 0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "type"));
        if (version <= 0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0 expected, got {1}", "version", version));

        SimplePrimitiveId pid = new SimplePrimitiveId(id, type);
        ArrayList<HistoryOsmPrimitive> versions = data.get(pid);
        if (versions == null)
            return null;
        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id, version))
                return primitive;
        }
        return null;
    }

    /**
     * Adds a history primitive to the data set
     *
     * @param primitive  the history primitive to add
     */
    public void put(HistoryOsmPrimitive primitive) {
        SimplePrimitiveId id = new SimplePrimitiveId(primitive.getId(), primitive.getType());
        if (data.get(id) == null) {
            data.put(id, new ArrayList<HistoryOsmPrimitive>());
        }
        data.get(id).add(primitive);
        fireHistoryUpdated(id);
    }

    /**
     * Replies the history for a given primitive with id <code>id</code>
     * and type <code>type</code>.
     *
     * @param id the id the if of the primitive. > 0 required
     * @param type the type of the primitive. Must not be null.
     * @return the history. null, if there isn't a history for <code>id</code> and
     * <code>type</code>.
     * @throws IllegalArgumentException thrown if id <= 0
     * @throws IllegalArgumentException thrown if type is null
     */
    public History getHistory(long id, OsmPrimitiveType type) throws IllegalArgumentException{
        if (id <= 0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0 expected, got {1}", "id", id));
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "type"));
        SimplePrimitiveId pid = new SimplePrimitiveId(id, type);
        return getHistory(pid);
    }

    /**
     * Replies the history for a primitive with id <code>id</code>. null, if no
     * such history exists.
     *
     * @param pid the primitive id. Must not be null.
     * @return the history for a primitive with id <code>id</code>. null, if no
     * such history exists
     * @throws IllegalArgumentException thrown if pid is null
     */
    public History getHistory(PrimitiveId pid) throws IllegalArgumentException{
        if (pid == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "pid"));
        ArrayList<HistoryOsmPrimitive> versions = data.get(pid);
        if (versions == null)
            return null;
        return new History(pid.getUniqueId(), pid.getType(), versions);
    }

    /**
     * merges the histories from the {@see HistoryDataSet} other in this history data set
     *
     * @param other the other history data set. Ignored if null.
     */
    public void mergeInto(HistoryDataSet other) {
        if (other == null)
            return;
        for (PrimitiveId id : other.data.keySet()) {
            this.data.put(id, other.data.get(id));
        }
        fireHistoryUpdated(null);
    }
}
