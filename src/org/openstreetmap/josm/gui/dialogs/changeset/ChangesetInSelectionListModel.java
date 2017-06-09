// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Collection;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;

/**
 * A table of changesets that displays the ones that are used by the primitives in the current selection.
 */
public class ChangesetInSelectionListModel extends ChangesetListModel implements SelectionChangedListener, ActiveLayerChangeListener {

    /**
     * Create a new {@link ChangesetInSelectionListModel}
     * @param selectionModel The model
     */
    public ChangesetInSelectionListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface SelectionChangeListener                                            */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        initFromPrimitives(newSelection);
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface LayerChangeListener                                                */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        DataSet newData = e.getSource().getEditDataSet();
        if (newData == null) {
            setChangesets(null);
        } else {
            initFromPrimitives(newData.getAllSelected());
        }
    }
}
