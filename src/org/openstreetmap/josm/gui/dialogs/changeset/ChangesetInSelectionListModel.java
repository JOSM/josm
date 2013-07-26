// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Collection;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class ChangesetInSelectionListModel extends ChangesetListModel implements SelectionChangedListener, EditLayerChangeListener{

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
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        if (newLayer == null) {
            setChangesets(null);
        } else {
            initFromPrimitives((newLayer).data.getAllSelected());
        }
    }
}
