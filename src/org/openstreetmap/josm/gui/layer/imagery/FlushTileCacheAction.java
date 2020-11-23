// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Flush tile cache.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class FlushTileCacheAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code FlushTileCacheAction}.
     * @param layer imagery layer
     */
    public FlushTileCacheAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Flush tile cache"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        new PleaseWaitRunnable(tr("Flush tile cache")) {
            @Override
            protected void realRun() {
                layer.clearTileCache();
                layer.invalidate();
            }

            @Override
            protected void finish() {
                // empty - flush is instantaneous
            }

            @Override
            protected void cancel() {
                // empty - flush is instantaneous
            }
        }.run();
    }
}
