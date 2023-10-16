// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Load all tiles.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class LoadAllTilesAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code LoadAllTilesAction}.
     * @param layer imagery layer
     */
    public LoadAllTilesAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Load all tiles"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        layer.loadAllTiles(true);
    }
}
