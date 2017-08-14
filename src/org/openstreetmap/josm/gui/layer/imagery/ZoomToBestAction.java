// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Change resolution to best zoom level.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class ZoomToBestAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code ZoomToBestAction}.
     * @param layer imagery layer
     */
    public ZoomToBestAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Change resolution"));
        this.layer = layer;
        setEnabled(!layer.getDisplaySettings().isAutoZoom() && layer.getBestZoom() != layer.getZoomLevel());
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        layer.setZoomLevel(layer.getBestZoom());
    }
}
