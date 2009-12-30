// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

public interface HistoryNameFormatter {
    String format(HistoryNode node);
    String format(HistoryWay node);
    String format(HistoryRelation node);
}
