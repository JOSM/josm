// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
public class CycleLayerUpAction extends JosmAction {
    private static final long serialVersionUID = 1L;
    private static final Shortcut cycleUp =
            Shortcut.registerShortcut("core:cyclelayerup", tr("Cycle layer up"), KeyEvent.VK_OPEN_BRACKET, Shortcut.SHIFT);

    /**
     * Create a CycleLayerDownAction that cycles through layers that are in the model
     */
    public CycleLayerUpAction() {
        super(tr("Cycle layer up"), "dialogs/next", tr("Cycle through data layers in an upward direction"),
                cycleUp, true, "cycle-layer-up", false);
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
        int index = managerLayers.indexOf(manager.getActiveLayer());
        int sublist = index < managerLayers.size() ? index + 1 : index;
        if (index >= managerLayers.size() - 1) {
            sublist = 0;
        }
        List<Layer> layers = managerLayers.subList(sublist, managerLayers.size());
        manager.setActiveLayer(layers.stream().filter(Layer::isVisible).filter(tlayer -> !(tlayer instanceof ImageryLayer))
                .findFirst().orElse(manager.getActiveLayer()));
    }
}
