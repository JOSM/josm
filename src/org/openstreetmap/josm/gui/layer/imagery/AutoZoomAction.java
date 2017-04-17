// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;

/**
 * Auto zoom.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class AutoZoomAction extends AbstractAction implements LayerAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code AutoZoomAction}.
     * @param layer imagery layer
     */
    public AutoZoomAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Auto zoom"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        TileSourceDisplaySettings settings = layer.getDisplaySettings();
        settings.setAutoZoom(!settings.isAutoZoom());
    }

    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
        item.setSelected(layer.getDisplaySettings().isAutoZoom());
        return item;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return AbstractTileSourceLayer.actionSupportLayers(layers);
    }
}
