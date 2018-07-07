// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.tools.KeyboardUtils;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Reorders all non-overlay imagery layers in a cyclic manner.
 * @since 13891
 */
public class ReorderImageryLayersAction extends JosmAction {

    /**
     * Constructs a new {@code ReorderImageryLayersAction}.
     */
    public ReorderImageryLayersAction() {
        // TODO: find a suitable icon
        super(tr("Reorder imagery layers"), "dialogs/layerlist", tr("Reorders non-overlay imagery layers."),
            Shortcut.registerMultiShortcuts("imagery:reorder", tr("Reorder imagery layers"),
                    KeyboardUtils.getCharactersForKey('E', 0), Shortcut.DIRECT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<ImageryLayer> layers = getNonOverlayImageryLayers().collect(Collectors.toList());
        int size = layers.size();
        if (size > 1) {
            // Move the first non-overlay layer at the position of the last non-overlay layer
            getLayerManager().moveLayer(layers.get(0),
                    getLayerManager().getLayers().indexOf(layers.get(size - 1)));
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getNonOverlayImageryLayers().count() > 1);
    }

    private Stream<ImageryLayer> getNonOverlayImageryLayers() {
        return getLayerManager().getLayersOfType(ImageryLayer.class).stream()
                .filter(l -> !l.getInfo().isOverlay());
    }
}
