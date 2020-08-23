// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The action to activate the currently selected layer
 */
public final class ActivateLayerAction extends AbstractAction
implements IEnabledStateUpdating, ActiveLayerChangeListener, MultikeyShortcutAction {
    private transient Layer layer;
    private final transient Shortcut multikeyShortcut;
    private final LayerListModel model;

    /**
     * Constructs a new {@code ActivateLayerAction}.
     * @param layer the layer
     * @param model layer list model
     */
    public ActivateLayerAction(Layer layer, LayerListModel model) {
        this(model);
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        this.layer = layer;
        putValue(NAME, tr("Activate"));
        updateEnabledState();
    }

    /**
     * Constructs a new {@code ActivateLayerAction}.
     * @param model layer list model
     */
    public ActivateLayerAction(LayerListModel model) {
        this.model = model;
        putValue(NAME, tr("Activate"));
        new ImageProvider("dialogs", "activate").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Activate the selected layer"));
        multikeyShortcut = Shortcut.registerShortcut("core_multikey:activateLayer", tr("Multikey: {0}",
                tr("Activate layer")), KeyEvent.VK_A, Shortcut.SHIFT);
        multikeyShortcut.setAccelerator(this);
        putValue("help", HelpUtil.ht("/Dialog/LayerList#ActivateLayer"));
    }

    @Override
    public Shortcut getMultikeyShortcut() {
        return multikeyShortcut;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Layer toActivate;
        if (layer != null) {
            toActivate = layer;
        } else {
            List<Layer> layers = model.getSelectedLayers();
            if (layers.isEmpty()) {
                // see #19476 for a possible cause
                return;
            }
            toActivate = layers.get(0);
        }
        execute(toActivate);
    }

    private void execute(Layer layer) {
        // model is going to be updated via LayerChangeListener and PropertyChangeEvents
        model.getLayerManager().setActiveLayer(layer);
        layer.setVisible(true);
    }

    boolean isActiveLayer(Layer layer) {
        return model.getLayerManager().getActiveLayer() == layer;
    }

    @Override
    public void updateEnabledState() {
        GuiHelper.runInEDTAndWait(() -> {
            if (layer == null) {
                if (model.getSelectedLayers().size() != 1) {
                    setEnabled(false);
                    return;
                }
                setEnabled(!isActiveLayer(model.getSelectedLayers().get(0)));
            } else {
                setEnabled(!isActiveLayer(layer));
            }
        });
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        updateEnabledState();
    }

    @Override
    public void executeMultikeyAction(int index, boolean repeat) {
        Layer l = LayerListDialog.getLayerForIndex(index);
        if (l != null) {
            execute(l);
        }
    }

    @Override
    public List<MultikeyInfo> getMultikeyCombinations() {
        return LayerListDialog.getLayerInfoByClass(Layer.class);
    }

    @Override
    public MultikeyInfo getLastMultikeyAction() {
        return null; // Repeating action doesn't make much sense for activating
    }

}
