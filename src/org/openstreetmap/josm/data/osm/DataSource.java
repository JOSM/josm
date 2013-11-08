// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A data source, defined by bounds and textual description for the origin.
 * @since 247
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
}
