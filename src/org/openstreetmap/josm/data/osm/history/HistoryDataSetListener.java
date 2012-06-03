// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import org.openstreetmap.josm.data.osm.PrimitiveId;

public interface HistoryDataSetListener {
    /**
     * Fired by a {@link HistoryDataSet} if the cached history of an OSM primitive with
     * id <code>id</code> is updated
     *
     * @param source the data set firing the event
     * @param id the id of the updated primitive
     */
    void historyUpdated(HistoryDataSet source, PrimitiveId id);

    /**
     * Fired by a {@link HistoryDataSet} if the history cached is cleared.
     *
     * @param source the history data set firing the event
     */
    void historyDataSetCleared(HistoryDataSet source);
}
