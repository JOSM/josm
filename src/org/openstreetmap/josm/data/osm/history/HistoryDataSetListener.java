// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import org.openstreetmap.josm.data.osm.PrimitiveId;

public interface HistoryDataSetListener {
    void historyUpdated(HistoryDataSet source, PrimitiveId id);
}
