// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A data source, defined by bounds and textual description for the origin.
 * @since 247 (creation)
 * @since 7575 (moved package)
 */
public class DataSource {

    /**
     * The bounds of this data source
     */
    public final Bounds bounds;

    /**
     * The textual description of the origin (example: "OpenStreetMap Server")
     */
    public final String origin;

    /**
     * Constructs a new {@code DataSource}.
     * @param bounds The bounds of this data source
     * @param origin The textual description of the origin (example: "OpenStreetMap Server")
     * @throws IllegalArgumentException if bounds is {@code null}
     */
    public DataSource(Bounds bounds, String origin) {
        CheckParameterUtil.ensureParameterNotNull(bounds, "bounds");
        this.bounds = bounds;
        this.origin = origin;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
        result = prime * result + ((origin == null) ? 0 : origin.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataSource other = (DataSource) obj;
        if (bounds == null) {
            if (other.bounds != null)
                return false;
        } else if (!bounds.equals(other.bounds))
            return false;
        if (origin == null) {
            if (other.origin != null)
                return false;
        } else if (!origin.equals(other.origin))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DataSource [bounds=" + bounds + ", origin=" + origin + ']';
    }

    /**
     * Returns the total area of downloaded data (the "yellow rectangles").
     * @param dataSources list of data sources
     * @return Area object encompassing downloaded data.
     * @see Data#getDataSourceArea()
     */
    public static Area getDataSourceArea(Collection<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return null;
        }
        Area a = new Area();
        for (DataSource source : dataSources) {
            // create area from data bounds
            a.add(new Area(source.bounds.asRect()));
        }
        return a;
    }

    /**
     * <p>Replies the list of data source bounds.</p>
     *
     * <p>Dataset maintains a list of data sources which have been merged into the
     * data set. Each of these sources can optionally declare a bounding box of the
     * data it supplied to the dataset.</p>
     *
     * <p>This method replies the list of defined (non {@code null}) bounding boxes.</p>
     * @param dataSources list of data sources
     *
     * @return the list of data source bounds. An empty list, if no non-null data source
     * bounds are defined.
     * @see Data#getDataSourceBounds()
     */
    public static List<Bounds> getDataSourceBounds(Collection<DataSource> dataSources) {
        if (dataSources == null) {
            return null;
        }
        List<Bounds> ret = new ArrayList<>(dataSources.size());
        for (DataSource ds : dataSources) {
            if (ds.bounds != null) {
                ret.add(ds.bounds);
            }
        }
        return ret;
    }
}
