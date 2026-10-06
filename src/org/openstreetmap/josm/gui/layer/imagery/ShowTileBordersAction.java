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
 * Toggles the drawing of a thin border around each tile of an imagery layer.
 * @since 19640
 */
public class ShowTileBordersAction extends AbstractAction implements LayerAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code ShowTileBordersAction}.
     * @param layer imagery layer
     */
    public ShowTileBordersAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Show tile borders"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        TileSourceDisplaySettings settings = layer.getDisplaySettings();
        boolean show = !settings.isShowTileBorders();
        settings.setShowTileBorders(show);
        // remember the choice, so that it also applies to layers created later and after a restart. Only the
        // action does this, not the setter, so that loading a session does not overwrite the preference.
        TileSourceDisplaySettings.PROP_SHOW_TILE_BORDERS.put(show);
    }

    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
        item.setSelected(layer.getDisplaySettings().isShowTileBorders());
        return item;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return AbstractTileSourceLayer.actionSupportLayers(layers);
    }
}
