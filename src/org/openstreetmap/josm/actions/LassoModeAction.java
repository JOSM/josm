// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Lasso selection mode: select objects within a hand-drawn region.
 * @since 5152
 */
public class LassoModeAction extends MapMode {

    /**
     * Constructs a new {@code LassoModeAction}.
     */
    public LassoModeAction() {
        super(tr("Lasso Mode"),
                /* ICON(mapmode/) */ "rope",
                tr("Lasso selection mode: select objects within a hand-drawn region"),
                ImageProvider.getCursor("normal", "rope"));
    }

    @Override
    public void enterMode() {
        if (Main.isDisplayingMapView()) {
            Main.map.mapModeSelect.setLassoMode(true);
            Main.map.mapModeSelect.enterMode();
        }
        super.enterMode();
    }

    @Override
    public void exitMode() {
        if (Main.isDisplayingMapView()) {
            Main.map.mapModeSelect.setLassoMode(false);
            Main.map.mapModeSelect.exitMode();
        }
        super.exitMode();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return Main.map.mapModeSelect.layerIsSupported(l);
    }
}
