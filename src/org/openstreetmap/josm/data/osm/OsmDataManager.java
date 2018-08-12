// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Global OSM dataset registry.
 * @since 14143
 */
public final class OsmDataManager implements IOsmDataManager {

    private OsmDataManager() {
        // hide constructor
    }

    private static class InstanceHolder {
        static final OsmDataManager INSTANCE = new OsmDataManager();
    }

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static OsmDataManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Collection<OsmPrimitive> getInProgressSelection() {
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapMode instanceof DrawAction) {
            return ((DrawAction) map.mapMode).getInProgressSelection();
        } else {
            DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
            if (ds == null) return Collections.emptyList();
            return ds.getSelected();
        }
    }

    @Override
    public Collection<? extends IPrimitive> getInProgressISelection() {
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapMode instanceof DrawAction) {
            return ((DrawAction) map.mapMode).getInProgressSelection();
        } else {
            OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            if (ds == null) return Collections.emptyList();
            return ds.getSelected();
        }
    }

    @Override
    public DataSet getEditDataSet() {
        return MainApplication.getLayerManager().getEditDataSet();
    }

    @Override
    public DataSet getActiveDataSet() {
        return MainApplication.getLayerManager().getActiveDataSet();
    }

    @Override
    public void setActiveDataSet(DataSet ds) {
        Optional<OsmDataLayer> layer = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).stream()
                .filter(l -> l.data.equals(ds)).findFirst();
        if (layer.isPresent()) {
            MainApplication.getLayerManager().setActiveLayer(layer.get());
        }
    }

    @Override
    public boolean containsDataSet(DataSet ds) {
        return MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).stream().anyMatch(l -> l.data.equals(ds));
    }
}
