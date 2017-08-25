// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action to merge the currently selected layer into another layer.
 */
public final class MergeAction extends AbstractAction implements IEnabledStateUpdating, LayerAction, Layer.MultiLayerAction {
    private transient Layer layer;
    private transient List<Layer> layers;
    private final LayerListModel model;

    /**
     * Constructs a new {@code MergeAction}.
     * @param layer the layer
     * @param model layer list model
     * @throws IllegalArgumentException if {@code layer} is null
     */
    public MergeAction(Layer layer, LayerListModel model) {
        this(layer, null, model);
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
    }

    /**
     * Constructs a new {@code MergeAction}.
     * @param layers the layer list
     * @param model layer list model
     * @throws IllegalArgumentException if {@code layers} is null
     */
    public MergeAction(List<Layer> layers, LayerListModel model) {
        this(null, layers, model);
        CheckParameterUtil.ensureParameterNotNull(layers, "layers");
    }

    /**
     * Constructs a new {@code MergeAction}.
     * @param layer the layer (null if layer list if specified)
     * @param layers the layer list (null if a single layer is specified)
     * @param model layer list model
     */
    private MergeAction(Layer layer, List<Layer> layers, LayerListModel model) {
        this.layer = layer;
        this.layers = layers;
        this.model = model;
        putValue(NAME, tr("Merge"));
        new ImageProvider("dialogs", "mergedown").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Merge this layer into another layer"));
        putValue("help", HelpUtil.ht("/Dialog/LayerList#MergeLayer"));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MergeLayerAction mergeLayerAction = MainApplication.getMenu().merge;
        if (layer != null) {
            mergeLayerAction.merge(layer);
        } else if (layers != null) {
            mergeLayerAction.merge(layers);
        } else {
            if (model.getSelectedLayers().size() == 1) {
                Layer selectedLayer = model.getSelectedLayers().get(0);
                mergeLayerAction.merge(selectedLayer);
            } else {
                mergeLayerAction.merge(model.getSelectedLayers());
            }
        }
    }

    @Override
    public void updateEnabledState() {
        if (layer == null && layers == null) {
            if (model.getSelectedLayers().isEmpty()) {
                setEnabled(false);
            } else if (model.getSelectedLayers().size() > 1) {
                setEnabled(supportLayers(model.getSelectedLayers()));
            } else {
                Layer selectedLayer = model.getSelectedLayers().get(0);
                List<Layer> targets = model.getPossibleMergeTargets(selectedLayer);
                setEnabled(!targets.isEmpty());
            }
        } else if (layer != null) {
            List<Layer> targets = model.getPossibleMergeTargets(layer);
            setEnabled(!targets.isEmpty());
        } else {
            setEnabled(supportLayers(layers));
        }
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        if (layers.isEmpty()) {
            return false;
        } else {
            final Layer firstLayer = layers.get(0);
            final List<Layer> remainingLayers = layers.subList(1, layers.size());
            return model.getPossibleMergeTargets(firstLayer).containsAll(remainingLayers);
        }
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public MergeAction getMultiLayerAction(List<Layer> layers) {
        return new MergeAction(layers, model);
    }
}
