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
    private List<OsmPrimitive> cache;

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
        DataSet.addSelectionListener(this);
        MapView.addLayerChangeListener(this);
    }

    public void unregister() {
        DataSet.removeSelectionListener(this);
        MapView.removeLayerChangeListener(this);
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public int getRowCount() {
        if (Main.main.getEditLayer() != layer)
            return 0;
        return cache.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return cache.get(rowIndex);
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer  == layer) {
            cache.clear();
        }
        if (newLayer == layer) {
            cache.addAll(((OsmDataLayer)newLayer).data.getAllSelected());
        }
        fireTableDataChanged();
    }

    @Override
    public void layerAdded(Layer newLayer) {
        // do nothing
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer == layer) {
            unregister();
        }
        this.cache.clear();
        fireTableDataChanged();
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (layer == Main.main.getEditLayer()) {
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
        selectionChanged(layer.data.getAllSelected());
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
