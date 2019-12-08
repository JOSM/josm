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

    @Override
    public void lock() {
        getDataSet().lock();
    }

    @Override
    public void unlock() {
        getDataSet().unlock();
    }

    @Override
    public boolean isLocked() {
        return getDataSet().isLocked();
    }

    /**
     * Clears the data backing this layer, unless if locked.
     * @since 15565
     */
    public void clear() {
        OsmData<?, ?, ?, ?> data = getDataSet();
        if (data != null && !data.isLocked()) {
            data.clear();
        }
    }
}
