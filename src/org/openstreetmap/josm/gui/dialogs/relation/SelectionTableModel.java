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
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class SelectionTableModel extends AbstractTableModel implements SelectionChangedListener, MapView.LayerChangeListener{

    /** this selection table model only displays selected primitives in this layer */
    private OsmDataLayer layer;
    private ArrayList<OsmPrimitive> cache;

    /**
     * constructor
     *
     * @param layer  the data layer. Must not be null.
     * @exception IllegalArgumentException thrown if layer is null
     */
    public SelectionTableModel(OsmDataLayer layer) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        cache = new ArrayList<OsmPrimitive>();
        populateSelectedPrimitives(layer);
    }

    public void register() {
        DataSet.selListeners.add(this);
        MapView.addLayerChangeListener(this);
    }

    public void unregister() {
        DataSet.selListeners.remove(this);
        MapView.removeLayerChangeListener(this);
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

    public List<OsmPrimitive> getSelection() {
        return cache;
    }

    /**
     * populates the model with the primitives currently selected in
     * <code>layer</code>
     *
     * @param layer  the data layer
     */
    protected void populateSelectedPrimitives(OsmDataLayer layer) {
        selectionChanged(layer.data.getSelected());
    }

    /**
     * Replies the primitive at row <code>row</code> in this model
     *
     * @param row the row
     * @return  the primitive at row <code>row</code> in this model
     */
    public OsmPrimitive getPrimitive(int row) {
        return cache.get(row);
    }
}
