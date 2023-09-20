// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action to delete the currently selected layer
 */
public final class DeleteLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {

    private final LayerListModel model;

    /**
     * Creates a {@link DeleteLayerAction} which will delete the currently selected layers in the layer dialog.
     * @param model layer list model
     */
    public DeleteLayerAction(LayerListModel model) {
        this.model = model;
        new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Delete the selected layers."));
        putValue(NAME, tr("Delete"));
        putValue("help", HelpUtil.ht("/Dialog/LayerList#DeleteLayer"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<Layer> selectedLayers = model.getSelectedLayers();
        if (selectedLayers.isEmpty())
            return;
        if (!SaveLayersDialog.saveUnsavedModifications(selectedLayers, SaveLayersDialog.Reason.DELETE))
            return;
        final Map<Integer, Layer> layerMap = model.selectedIndices().filter(i -> model.getLayer(i) != null)
                .collect(HashMap::new, (map, value) -> map.put(value, model.getLayer(value)), HashMap::putAll);
        for (Layer l: selectedLayers) {
            if (model.getLayerManager().containsLayer(l)) {
                // it may happen that this call removes other layers.
                // this is why we need to check if every layer is still in the list of selected layers.
                model.getLayerManager().removeLayer(l);
            }
        }
        // Set the next active layer to the next visible layer
        if (layerMap.size() == 1) {
            final int selected = Math.min(layerMap.keySet().iterator().next(), model.getRowCount() - 1);
            int currentLayerIndex = selected;
            Layer layer = model.getLayer(currentLayerIndex);
            // If the user has the last layer selected, we need to wrap around.
            boolean reversed = false;
            while (layer != null && !layer.isVisible() && currentLayerIndex < model.getRowCount() && currentLayerIndex >= 0) {
                if (reversed) {
                    currentLayerIndex--;
                } else {
                    currentLayerIndex++;
                }
                if (currentLayerIndex == model.getRowCount()) {
                    reversed = true;
                    currentLayerIndex = selected;
                }
                layer = model.getLayer(currentLayerIndex);
            }
            if (layer != null) {
                model.getLayerManager().setActiveLayer(layer);
                // Reset the selection
                model.getSelectionModel().setSelectionInterval(selected, selected);
            }
        }
    }

    @Override
    public void updateEnabledState() {
        setEnabled(!model.getSelectedLayers().isEmpty());
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DeleteLayerAction;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
