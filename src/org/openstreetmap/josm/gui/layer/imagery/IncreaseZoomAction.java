// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Increase zoom.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class IncreaseZoomAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code IncreaseZoomAction}.
     * @param layer imagery layer
     */
    public IncreaseZoomAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Increase zoom"));
        this.layer = layer;
        setEnabled(!layer.getDisplaySettings().isAutoZoom() && layer.zoomIncreaseAllowed());
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        layer.increaseZoomLevel();
    }
}
