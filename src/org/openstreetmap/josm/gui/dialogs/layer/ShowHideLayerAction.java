// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.util.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action which will toggle the visibility of the currently selected layers.
 */
public final class ShowHideLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction, MultikeyShortcutAction {

    private transient WeakReference<Layer> lastLayer;
    private final transient Shortcut multikeyShortcut;
    private final LayerListModel model;

    /**
     * Creates a {@link ShowHideLayerAction} which will toggle the visibility of the currently selected layers
     * @param model layer list model
     */
    public ShowHideLayerAction(LayerListModel model) {
        this.model = model;
        putValue(NAME, tr("Show/hide"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "showhide"));
        putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the selected layer."));
        putValue("help", HelpUtil.ht("/Dialog/LayerList#ShowHideLayer"));
        multikeyShortcut = Shortcut.registerShortcut("core_multikey:showHideLayer", tr("Multikey: {0}",
                tr("Show/hide layer")), KeyEvent.VK_S, Shortcut.SHIFT);
        multikeyShortcut.setAccelerator(this);
        updateEnabledState();
    }

    @Override
    public Shortcut getMultikeyShortcut() {
        return multikeyShortcut;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Layer l : model.getSelectedLayers()) {
            l.toggleVisible();
        }
    }

    @Override
    public void executeMultikeyAction(int index, boolean repeat) {
        Layer l = LayerListDialog.getLayerForIndex(index);
        if (l != null) {
            l.toggleVisible();
            lastLayer = new WeakReference<>(l);
        } else if (repeat && lastLayer != null) {
            l = lastLayer.get();
            if (LayerListDialog.isLayerValid(l)) {
                l.toggleVisible();
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
        return obj instanceof ShowHideLayerAction;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public List<MultikeyInfo> getMultikeyCombinations() {
        return LayerListDialog.getLayerInfoByClass(Layer.class);
    }

    @Override
    public MultikeyInfo getLastMultikeyAction() {
        if (lastLayer != null)
            return LayerListDialog.getLayerInfo(lastLayer.get());
        return null;
    }
}
