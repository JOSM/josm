// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class HistoryDataSet {

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
            throw new NoSuchElementException(tr("Didn't find an historized primitive with id {0} in this dataset", id));
        return new History(id, versions);
    }
}
