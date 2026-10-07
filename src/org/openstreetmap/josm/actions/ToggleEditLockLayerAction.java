// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.data.osm.Lockable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action enabling/disabling the {@linkplain Lockable#lock() read-only flag}
 * of the layer specified in the constructor.
 * @param <L> Type of layer the action should be instantiated for
 * 
 * @since xxx
 */
public class ToggleEditLockLayerAction<L extends Layer & Lockable> extends AbstractAction implements LayerAction {

    private final L layer;

    /**
     * Construct a new {@code ToggleEditLockLayerAction}
     * @param layer the layer for which to toggle the {@linkplain Lockable#lock() read-only flag}
     * 
     * @since xxx
     */
    public ToggleEditLockLayerAction(L layer) {
        super(tr("Prevent modification"));
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        putValue(SHORT_DESCRIPTION, tr("Prevent/allow changes being made in this layer"));
        new ImageProvider("lock").getResource().attachImageIcon(this, true);
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (layer.isLocked()) {
            layer.unlock();
        } else {
            layer.lock();
        }

        layer.invalidate();
        LayerListDialog.getInstance().repaint();
    }

    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
        item.setSelected(layer.isLocked());
        return item;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof Lockable;
    }
}
