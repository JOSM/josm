// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action to duplicate the given selected layer into another layer.
 */
public final class DuplicateAction extends AbstractAction implements IEnabledStateUpdating {
    private transient Layer layer;
    private final LayerListModel model;

    /**
     * Constructs a new {@code DuplicateAction}.
     * @param layer the layer
     * @param model layer list model
     * @throws IllegalArgumentException if {@code layer} is null
     */
    public DuplicateAction(Layer layer, LayerListModel model) {
        this(model);
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        updateEnabledState();
    }

    /**
     * Constructs a new {@code DuplicateAction}.
     * @param model layer list model
     */
    public DuplicateAction(LayerListModel model) {
        this.model = model;
        putValue(NAME, tr("Duplicate"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "duplicatelayer"));
        putValue(SHORT_DESCRIPTION, tr("Duplicate this layer"));
        putValue("help", HelpUtil.ht("/Dialog/LayerList#DuplicateLayer"));
        updateEnabledState();
    }

    private static void duplicate(Layer layer) {
        if (layer instanceof OsmDataLayer) {
            String newName = LayerListTransferHandler.suggestNewLayerName(layer.getName(), MainApplication.getLayerManager().getLayers());
            OsmDataLayer oldLayer = (OsmDataLayer) layer;
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(oldLayer.data), newName, null));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (layer != null) {
            duplicate(layer);
        } else {
            duplicate(model.getSelectedLayers().get(0));
        }
    }

    @Override
    public void updateEnabledState() {
        if (layer == null) {
            if (model != null && model.getSelectedLayers().size() == 1) {
                setEnabled(model.getSelectedLayers().get(0) instanceof OsmDataLayer);
            } else {
                setEnabled(false);
            }
        } else {
            setEnabled(layer instanceof OsmDataLayer);
        }
    }
}
