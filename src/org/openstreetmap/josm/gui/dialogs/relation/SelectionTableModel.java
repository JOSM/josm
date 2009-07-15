// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;

public class SelectionTableModel extends AbstractTableModel implements SelectionChangedListener, LayerChangeListener{

    /** this selection table model only displays selected primitives in this layer */
    private OsmDataLayer layer;
    private ArrayList<OsmPrimitive> cache;

    public SelectionTableModel(OsmDataLayer layer) {
        this.layer = layer;
        cache = new ArrayList<OsmPrimitive>();
        DataSet.selListeners.add(this);
        Layer.listeners.add(this);
    }


    public void unregister() {
        DataSet.selListeners.remove(this);
        Layer.listeners.remove(this);
    }

    public int getColumnCount() {
        return 1;
    }

    public int getRowCount() {
        if (Main.map.mapView.getEditLayer() != layer)
            return 0;
        return cache.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return cache.get(rowIndex);
    }

    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer  == layer) {
            cache.clear();
        }
        if (newLayer == layer) {
            cache.addAll(((OsmDataLayer)newLayer).data.getSelected());
        }
        fireTableDataChanged();
    }

    public void layerAdded(Layer newLayer) {
        // do nothing
    }

    public void layerRemoved(Layer oldLayer) {
        if (oldLayer == layer) {
            unregister();
        }
        this.cache.clear();
        fireTableDataChanged();
    }

    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (layer == Main.map.mapView.getEditLayer()) {
            cache.clear();
            cache.addAll(newSelection);
        } else {
            cache.clear();
        }
        fireTableDataChanged();
    }

    public List<? extends OsmPrimitive> getSelection() {
        return cache;
    }
}
