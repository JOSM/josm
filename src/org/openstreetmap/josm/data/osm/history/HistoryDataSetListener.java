// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

public interface HistoryDataSetListener {
    void historyUpdated(HistoryDataSet source, long primitiveId);
}
