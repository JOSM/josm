// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * This is a listener that listens to selection change events in the data set.
 *
 * @author Taylor Smock
 * @since 15609
 */
@FunctionalInterface
public interface DataSourceListener {
    /**
     * Called whenever the data source list is changed.
     *
     * You get notified about the new data source list, the sources that were added
     * and removed and the dataset that triggered the event.
     *
     * @param event The data source change event.
     * @see DataSourceChangeEvent
     */
    void dataSourceChange(DataSourceChangeEvent event);
}
