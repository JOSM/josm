// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class HistoryDataSet {

    private static HistoryDataSet historyDataSet;

    public static HistoryDataSet getInstance() {
        if (historyDataSet == null) {
            historyDataSet = new HistoryDataSet();
        }
        return  historyDataSet;
    }

    private HashMap<Long, ArrayList<HistoryOsmPrimitive>> data;

    public HistoryDataSet() {
        data = new HashMap<Long, ArrayList<HistoryOsmPrimitive>>();
    }

    public HistoryOsmPrimitive get(long id, long version) {
        ArrayList<HistoryOsmPrimitive> versions = data.get(id);
        if (versions == null)
            throw new NoSuchElementException(tr("Didn't find an  primitive with id {0} in this dataset", id));

        for (HistoryOsmPrimitive primitive: versions) {
            if (primitive.matches(id, version))
                return primitive;
        }
        throw new NoSuchElementException(tr("Didn't find an primitive with id {0} and version {1} in this dataset", id, version));
    }

    public void put(HistoryOsmPrimitive primitive) {
        if (data.get(primitive.getId()) == null) {
            data.put(primitive.getId(), new ArrayList<HistoryOsmPrimitive>());
        }
        data.get(primitive.getId()).add(primitive);
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
            if (!this.data.keySet().contains(id)) {
                this.data.put(id, other.data.get(id));
            }
        }
    }
}
