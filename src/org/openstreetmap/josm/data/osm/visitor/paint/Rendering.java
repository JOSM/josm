// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmData;

/**
 * <p>An object which can render data provided by a {@link OsmData}.</p>
 * @since  4087 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface Rendering {
    /**
     * <p>Renders the OSM data in {@code data}</p>
     *
     * @param data the data set to be rendered
     * @param renderVirtualNodes if true, renders virtual nodes. Otherwise, ignores them.
     * @param bbox the bounding box for the data to be rendered. Only objects within or intersecting
     * with {@code bbox} are rendered
     * @since 13810 (signature)
     */
    void render(OsmData<?, ?, ?, ?> data, boolean renderVirtualNodes, Bounds bbox);
}
