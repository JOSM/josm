// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Allow users to cycle between adjacent layers easily
 *
 * @author Taylor Smock
 * @since 15923
 */
public class CycleLayerDownAction extends JosmAction {
    private static final long serialVersionUID = 1L;
    private static final Shortcut cycleDown =
            Shortcut.registerShortcut("core:cyclelayerdown", tr("Cycle layers down"), KeyEvent.VK_CLOSE_BRACKET, Shortcut.SHIFT);

    /**
     * Create a CycleLayerDownAction that cycles through layers that are in the model
     */
    public CycleLayerDownAction() {
        super(tr("Cycle layers"), "dialogs/next", tr("Cycle through layers"), cycleDown, true, "cycle-layer", false);
        new ImageProvider("dialogs", "next").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Cycle through visible layers."));
        putValue(NAME, tr("Cycle layers"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainLayerManager manager = MainApplication.getLayerManager();
        List<Layer> managerLayers = manager.getLayers().stream().filter(layer -> !(layer instanceof ImageryLayer))
                .collect(Collectors.toList());
        if (managerLayers.isEmpty()) {
            return;
        }

        List<Layer> layers = new ArrayList<>(managerLayers);
        Collections.reverse(layers);
        int index = layers.indexOf(manager.getActiveLayer());
        int sublist = index < managerLayers.size() - 1 ? index + 1 : 0;
        layers = layers.subList(sublist, layers.size());

        manager.setActiveLayer(layers.stream().filter(Layer::isVisible).filter(tlayer -> !(tlayer instanceof ImageryLayer))
                .findFirst().orElse(manager.getActiveLayer()));
    }
}
