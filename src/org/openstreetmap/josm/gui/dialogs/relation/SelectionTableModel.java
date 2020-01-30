// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This table shows the primitives that are currently selected in the main OSM view.
 * @since 1790
 */
public class SelectionTableModel extends AbstractTableModel implements DataSelectionListener, ActiveLayerChangeListener, LayerChangeListener {

    /** this selection table model only displays selected primitives in this layer */
    private final transient OsmDataLayer layer;
    private final transient List<OsmPrimitive> cache;

    /**
     * Creates a new {@link SelectionTableModel} for a given layer
     *
     * @param layer  the data layer. Must not be null.
     * @throws IllegalArgumentException if layer is null
     */
    public SelectionTableModel(OsmDataLayer layer) {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        cache = new ArrayList<>();
        populateSelectedPrimitives(layer);
    }

    /**
     * Registers listeners (selection change and layer change).
     */
    public void register() {
        SelectionEventManager.getInstance().addSelectionListener(this);
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
    }

    /**
     * Unregisters listeners (selection change and layer change).
     */
    public void unregister() {
        SelectionEventManager.getInstance().removeSelectionListener(this);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public int getRowCount() {
        if (MainApplication.getLayerManager().getEditLayer() != layer)
            return 0;
        return cache.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return cache.get(rowIndex);
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        if (e.getPreviousActiveLayer() == layer) {
            cache.clear();
        }
        fireTableDataChanged();
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // do nothing
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() == layer) {
            unregister();
        }
        this.cache.clear();
        fireTableDataChanged();
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // do nothing
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        selectionChanged(event.getSelection());
    }

    private void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (layer == MainApplication.getLayerManager().getActiveDataLayer()) {
            cache.clear();
            cache.addAll(newSelection);
        } else {
            cache.clear();
        }
        fireTableDataChanged();
    }

    /**
     * Returns the selected primitives.
     * @return the selected primitives
     */
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
     * @return the primitive at row <code>row</code> in this model
     * @throws ArrayIndexOutOfBoundsException if index is invalid
     */
    public OsmPrimitive getPrimitive(int row) {
        return cache.get(row);
    }

}
