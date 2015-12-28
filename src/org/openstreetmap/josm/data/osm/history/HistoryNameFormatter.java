// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

/**
 * Formats a name for a {@link HistoryOsmPrimitive}.
 * @since 2686
 */
public interface HistoryNameFormatter {

    /**
     * Formats a name for a {@link HistoryNode}.
     *
     * @param node the node
     * @return the name
     */
    String format(HistoryNode node);

    /**
     * Formats a name for a {@link HistoryWay}.
     *
     * @param way the way
     * @return the name
     */
    String format(HistoryWay way);

    /**
     * Formats a name for a {@link HistoryRelation}.
     *
     * @param relation the relation
     * @return the name
     */
    String format(HistoryRelation relation);
}
