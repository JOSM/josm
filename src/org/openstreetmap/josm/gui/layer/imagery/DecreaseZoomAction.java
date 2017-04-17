// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Decrease zoom.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class DecreaseZoomAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code DecreaseZoomAction}.
     * @param layer imagery layer
     */
    public DecreaseZoomAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Decrease zoom"));
        this.layer = layer;
        setEnabled(!layer.getDisplaySettings().isAutoZoom() && layer.zoomDecreaseAllowed());
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        layer.decreaseZoomLevel();
    }
}
