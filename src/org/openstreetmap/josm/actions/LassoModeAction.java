// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
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
        if (MainApplication.isDisplayingMapView()) {
            MapFrame map = MainApplication.getMap();
            map.mapModeSelect.setLassoMode(true);
            map.mapModeSelect.enterMode();
        }
        super.enterMode();
    }

    @Override
    public void exitMode() {
        if (MainApplication.isDisplayingMapView()) {
            MapFrame map = MainApplication.getMap();
            map.mapModeSelect.setLassoMode(false);
            map.mapModeSelect.exitMode();
        }
        super.exitMode();
    }

    @Override
    public boolean layerIsSupported(Layer l) {
        return MainApplication.getMap().mapModeSelect.layerIsSupported(l);
    }
}
