// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class ChangesetsInActiveDataLayerListModel extends ChangesetListModel implements MapView.LayerChangeListener, DataChangeListener{

    public ChangesetsInActiveDataLayerListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface LayerChangeListener                                                */
    /* ---------------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer != null && oldLayer instanceof OsmDataLayer) {
            OsmDataLayer l = (OsmDataLayer)oldLayer;
            l.listenerDataChanged.remove(this);
        }
        if (newLayer == null) {
            setChangesets(null);
        } else if (newLayer instanceof OsmDataLayer){
            OsmDataLayer l = (OsmDataLayer)newLayer;
            l.listenerDataChanged.add(this);
            initFromDataSet(l.data);
        } else {
            setChangesets(null);
        }
    }
    public void layerAdded(Layer newLayer) {}
    public void layerRemoved(Layer oldLayer) {}

    /* ---------------------------------------------------------------------------- */
    /* Interface DataChangeListener                                                 */
    /* ---------------------------------------------------------------------------- */
    public void dataChanged(OsmDataLayer l) {
        if (l == null) return;
        if (l != Main.main.getEditLayer()) return;
        initFromDataSet(l.data);
    }
}
