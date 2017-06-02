// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Load all error tiles.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class LoadErroneousTilesAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code LoadErroneousTilesAction}.
     * @param layer imagery layer
     */
    public LoadErroneousTilesAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Load all error tiles"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        layer.loadAllErrorTiles(true);
    }
}
