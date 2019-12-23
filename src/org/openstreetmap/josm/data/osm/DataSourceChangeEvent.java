// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Set;

import org.openstreetmap.josm.data.DataSource;

/**
 * The event that is fired when the data source list is changed.
 *
 * @author Taylor Smock
 * @since 15609
 */
public interface DataSourceChangeEvent {
    /**
     * Gets the previous data source list
     * <p>
     * This collection cannot be modified and will not change.
     *
     * @return The old data source list
     */
    Set<DataSource> getOldDataSources();

    /**
     * Gets the new data sources. New data sources are added to the end of the
     * collection.
     * <p>
     * This collection cannot be modified and will not change.
     *
     * @return The new data sources
     */
    Set<DataSource> getDataSources();

    /**
     * Gets the Data Sources that have been removed from the selection.
     * <p>
     * Those are the primitives contained in {@link #getOldDataSources()} but not in
     * {@link #getDataSources()}
     * <p>
     * This collection cannot be modified and will not change.
     *
     * @return The DataSources that were removed
     */
    Set<DataSource> getRemoved();

    /**
     * Gets the data sources that have been added to the selection.
     * <p>
     * Those are the data sources contained in {@link #getDataSources()} but not in
     * {@link #getOldDataSources()}
     * <p>
     * This collection cannot be modified and will not change.
     *
     * @return The data sources that were added
     */
    Set<DataSource> getAdded();

    /**
     * Gets the data set that triggered this selection event.
     *
     * @return The data set.
     */
    DataSet getSource();

    /**
     * Test if this event did not change anything.
     * <p>
     * This will return <code>false</code> for all events that are sent to
     * listeners, so you don't need to test it.
     *
     * @return <code>true</code> if this did not change the selection.
     */
    default boolean isNop() {
        return getAdded().isEmpty() && getRemoved().isEmpty();
    }
}

