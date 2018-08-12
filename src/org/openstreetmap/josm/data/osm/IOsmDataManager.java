// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;

/**
 * Global OSM dataset registry.
 * @since 14143
 */
public interface IOsmDataManager {

    /**
     * Replies the current selected OSM primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link DataSet#getSelected()}.
     * @return The current selected OSM primitives, from a end-user point of view. Can be {@code null}.
     */
    Collection<OsmPrimitive> getInProgressSelection();

    /**
     * Replies the current selected primitives, from a end-user point of view.
     * It is not always technically the same collection of primitives than {@link OsmData#getSelected()}.
     * @return The current selected primitives, from a end-user point of view. Can be {@code null}.
     */
    Collection<? extends IPrimitive> getInProgressISelection();

    /**
     * Gets the active edit data set (not read-only).
     * @return That data set, <code>null</code>.
     * @see #getActiveDataSet
     */
    DataSet getEditDataSet();

    /**
     * Gets the active data set (can be read-only).
     * @return That data set, <code>null</code>.
     * @see #getEditDataSet
     */
    DataSet getActiveDataSet();

    /**
     * Sets the active data set (and also edit data set if not read-only).
     * @param ds New data set, or <code>null</code>
     */
    void setActiveDataSet(DataSet ds);

    /**
     * Determines if the list of data sets managed by JOSM contains {@code ds}.
     * @param ds the data set to look for
     * @return {@code true} if the list of data sets managed by JOSM contains {@code ds}
     */
    boolean containsDataSet(DataSet ds);
}
