// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Cosntructs a new {@link DataSource}
     * @param source The source to copy the data from.
     * @since 10346
     */
    public DataSource(DataSource source) {
        this(source.bounds, source.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bounds, origin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DataSource that = (DataSource) obj;
        return Objects.equals(bounds, that.bounds) &&
                Objects.equals(origin, that.origin);
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
        Path2D.Double p = new Path2D.Double();
        for (DataSource source : dataSources) {
            // create area from data bounds
            p.append(source.bounds.asRect(), false);
        }
        return new Area(p);
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
        return dataSources.stream()
                .filter(ds -> ds.bounds != null).map(ds -> ds.bounds)
                .collect(Collectors.toList());
    }
}
