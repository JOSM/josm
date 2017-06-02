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
 * Show tile errors.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class ShowErrorsAction extends AbstractAction implements LayerAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code ShowErrorsAction}.
     * @param layer imagery layer
     */
    public ShowErrorsAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Show errors"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        TileSourceDisplaySettings settings = layer.getDisplaySettings();
        settings.setShowErrors(!settings.isShowErrors());
    }

    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
        item.setSelected(layer.getDisplaySettings().isShowErrors());
        return item;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return AbstractTileSourceLayer.actionSupportLayers(layers);
    }
}
