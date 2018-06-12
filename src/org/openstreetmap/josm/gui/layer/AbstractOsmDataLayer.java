// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.data.osm.OsmData;

/**
 * Abstraction of {@link OsmDataLayer}.
 * @since 13926
 */
public abstract class AbstractOsmDataLayer extends AbstractModifiableLayer {

    protected AbstractOsmDataLayer(String name) {
        super(name);
    }

    /**
     * Returns the {@link OsmData} behind this layer.
     * @return the {@link OsmData} behind this layer.
     */
    public abstract OsmData<?, ?, ?, ?> getDataSet();
}
