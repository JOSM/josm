// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;

public class LassoModeAction extends MapMode {

    public LassoModeAction() {
        super(tr("Lasso Mode"),
                "rope",
                tr("Lasso selection mode: select objects within a hand-drawn region"),
                null,
                null);
    }

    @Override
    public void enterMode() {
        super.enterMode();
        if (Main.map != null) {
            Main.map.mapModeSelect.setLassoMode(true);
            Main.map.mapModeSelect.enterMode();
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        Main.map.mapModeSelect.setLassoMode(false);
        Main.map.mapModeSelect.exitMode();
    }
}
