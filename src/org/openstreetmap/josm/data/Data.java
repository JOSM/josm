// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.awt.geom.Area;
import java.util.Collection;
import java.util.List;

/**
 * Generic data, holding data downloaded from various data sources.
 * @since 7575
 */
@FunctionalInterface
public interface Data {

    /**
     * Returns the collection of data sources.
     * @return the collection of data sources.
     */
    Collection<DataSource> getDataSources();

    /**
     * Returns the total area of downloaded data (the "yellow rectangles").
     * @return Area object encompassing downloaded data.
     */
    default Area getDataSourceArea() {
        return DataSource.getDataSourceArea(getDataSources());
    }

    /**
     * <p>Replies the list of data source bounds.</p>
     *
     * <p>Dataset maintains a list of data sources which have been merged into the
     * data set. Each of these sources can optionally declare a bounding box of the
     * data it supplied to the dataset.</p>
     *
     * <p>This method replies the list of defined (non {@code null}) bounding boxes.</p>
     *
     * @return the list of data source bounds. An empty list, if no non-null data source
     * bounds are defined.
     */
    default List<Bounds> getDataSourceBounds() {
        return DataSource.getDataSourceBounds(getDataSources());
    }
}
